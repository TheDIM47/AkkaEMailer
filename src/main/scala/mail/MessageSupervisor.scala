package mail

import java.util.UUID
import javax.mail.MessagingException

import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.actor.{Actor, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import com.sun.mail.iap.ConnectionException
import com.typesafe.config.Config
import model.SimpleRequestProtocol.RequestT
import mq.MailerMap
import mq.RMQProtocol.AckMessageT
import org.slf4j.LoggerFactory

import scala.concurrent.duration.DurationDouble

object MessageSupervisor {
  def props(conf: Config) = Props(new MessageSupervisor(conf))
}

/**
  * Receive: [[model.SimpleRequestProtocol.Welcome]], [[model.SimpleRequestProtocol.ResetPassword]]
  * Create Mailer actor and send [[model.SimpleRequestProtocol.RequestT]] message for following processing inside mail actor.
  * @param conf Configuration
  */
class MessageSupervisor(conf: Config) extends Actor {
  private val log = LoggerFactory.getLogger(classOf[MessageSupervisor])
  private val mmap = new MailerMap[UUID, RequestT]()

  import model.SimpleRequestProtocol._

  /** Supervisor strategy - restart child actors on message/connection problems */
  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
    case _: MessagingException => Restart
    case _: ConnectionException => Restart
    case t => super.supervisorStrategy.decider.applyOrElse(t, (_: Any) => Escalate)
  }

  override def receive: Receive = {
    case msg: RequestT =>
      log.debug(s"Rmq: $msg")
      val mailer = context.actorOf(MailerActor.props(conf), name = s"mailer-actor-${msg.id}")
      context.watch(mailer)
      mmap.put(msg.id, (msg, mailer))
      mailer ! msg

    // Success - send ack back to rmq
    case msg: AckMessageT =>
      log.debug(s"Ack: $msg")
      mmap.remove(msg.id)

    // If actor removed from map - then it completed successfully, else - failure
    case Terminated(ref) =>
      mmap.findByRef(ref) match {
        case Some(entry) =>
          log.debug(s"Entry found: $entry")
          entry._2 ! entry._1
        case None =>
          context.unwatch(ref)
      }

    case x => log.error(s"Invalid message $x")
  }
}
