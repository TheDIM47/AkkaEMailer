package mq

import javax.mail.MessagingException

import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props, SupervisorStrategy, Terminated, TypedActor, TypedProps}
import com.sun.mail.iap.ConnectionException
import com.typesafe.config.Config
import mail.MailerActor
import mq.RMQProtocol.{AckMessage, RMQMessage}
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.duration.DurationLong

object RMQActor {
  def props(conf: Config) = Props(new RMQActor(conf))
}

class MailerMap {
  type ValueType = (RMQMessage, ActorRef)
  private val mmap = mutable.Map[Long, ValueType]()

  def findByRef(ref: ActorRef): Option[ValueType] = mmap.find({
    case (_, (_, r)) => r.equals(ref)
  }).map(_._2)

  def put(tag: Long, value: ValueType): Unit = mmap.put(tag, value)

  def remove(tag: Long): Unit = mmap.remove(tag)
}

class RMQActor(conf: Config) extends Actor {
  private val log = LoggerFactory.getLogger(classOf[RMQActor])
  private val listener: RMQHandlerT = TypedActor(context).typedActorOf(TypedProps(classOf[RMQHandlerT], new RMQHandler(conf, self)))
  private val mmap = new MailerMap

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
    // case _: com.sun.mail.util.MailConnectException => Restart
    case _: MessagingException => Restart
    case _: ConnectionException => Restart
    case t => super.supervisorStrategy.decider.applyOrElse(t, (_: Any) => Escalate)
  }

  override def postStop(): Unit = {
    listener.close()
    TypedActor(context).stop(listener)
  }

  override def receive: Receive = {
    case msg: RMQMessage =>
      log.debug(s"Rmq: $msg")
      val mailer = context.actorOf(MailerActor.props(conf), name = s"mailer-actor-${msg.tag}")
      context.watch(mailer)
      mmap.put(msg.tag, (msg, mailer))
      mailer ! msg

    // Success - send ack back to rmq
    case msg: AckMessage =>
      log.debug(s"Ack: $msg")
      mmap.remove(msg.tag)
      listener.ack(msg.tag)

    // If actor removed from map - then it completed successfully, else - failure
    case Terminated(ref) =>
      mmap.findByRef(ref) match {
        case Some(entry) =>
          log.debug(s"Entry found: $entry")
          entry._2 ! entry._1
        case None =>
          context.unwatch(ref)
      }
  }
}
