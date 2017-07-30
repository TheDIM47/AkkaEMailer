package mq

import akka.actor.{ActorRef, TypedActor}
import com.typesafe.config.Config
import mq.RMQProtocol._
import org.slf4j.LoggerFactory

object RMQProtocol {
  case class RMQMessage(tag: Long, message: String)
  case class AckMessage(tag: Long)
}

/**
  * Run RMQ consumer and read messages from Rabbit Queue or as Actor messages
  */
trait RMQHandlerT {
  def consume(tag: Long, msg: Array[Byte]): Unit
  def ack(tag: Long): Unit
  def close(): Unit
}

class RMQHandler(conf: Config, owner: ActorRef) extends RMQHandlerT {
  private val log = LoggerFactory.getLogger(classOf[RMQHandler])
  private val rmq = new RMQConnector(conf, consume)
  private val self = TypedActor.get(TypedActor.context).getActorRefFor(TypedActor.self)

  override def ack(tag: Long): Unit = {
    log.debug(s"Ack: $tag")
    rmq.ack(tag)
  }

  override def consume(tag: Long, msg: Array[Byte]): Unit = {
    val message = RMQMessage(tag, new String(msg))
    log.debug(s"Consumed message: $message")
    owner.tell(message, self)
  }

  override def close(): Unit = rmq.close()
}
