package mail

import akka.actor.{Actor, PoisonPill, Props}
import com.typesafe.config.Config
import model.RequestProtocol._
import model.SimpleRequestProtocol._
import mq.RMQProtocol._
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsError, JsSuccess, Json}

trait MessageSender {
  def sendMessage(conf: Config, event: RequestMessage): Unit
  def sendMessage(conf: Config, event: RequestT): Unit
}

trait MessageSenderImpl extends MessageSender {
  override def sendMessage(conf: Config, event: RequestMessage): Unit = {
    sendMessage(conf, event.toRequest)
  }

  override def sendMessage(conf: Config, event: RequestT): Unit = {
    val rb = new ReportBuilder(conf)
    val text = rb.build(event)
    val subj = event match {
      case _: Welcome => "registration confirmation"
      case _: ResetPassword => "reset password request"
    }
    val eb = new EmailBuilder()
      .from(conf.getString("mail.from"))
      .replyTo(conf.getString("mail.reply"))
      .to(s"${event.firstName} <${event.recipient}>")
      .cc(conf.getString("mail.cc"))
      .bcc(conf.getString("mail.bcc"))
      .withText(text)
      .subj(subj)
    new MailSender(conf).send(eb.build())
  }
}

object MailerActor {
  def props(conf: Config) = Props(new MailerActor(conf) with MessageSenderImpl)
}

/**
  * Create and send email, send ack back to owner, then die
  * Receive: [[mq.RMQProtocol.RMQMessage]]
  * Send: [[mq.RMQProtocol.AckMessage]]
  * @param conf Configuration
  */
abstract class MailerActor(conf: Config) extends Actor { that: MessageSender =>
  val log = LoggerFactory.getLogger(classOf[MailerActor])
  import model.RequestProtocol._
  log.debug(s"Starting send mail actor $self")

  override def postRestart(reason: Throwable): Unit = {
    super.postRestart(reason)
    log.debug(s"Restarting send mail actor $self due to ${reason.getMessage}")
  }

  override def receive: Receive = {
    case msg: RequestT =>
      sendMessage(conf, msg)
      sender() ! AckMessageT(msg.id)
      self ! PoisonPill

    case msg: RMQMessage =>
      Json.parse(msg.message).validate[RequestMessage] match {
        case JsSuccess(event, _) =>
          sendMessage(conf, event)
          log.info(s"Message [$msg] was sent")
        case JsError(err) =>
          log.error(s"Error parsing message [$msg] $err")
      }
      sender() ! AckMessage(msg.tag)
      self ! PoisonPill
  }
}
