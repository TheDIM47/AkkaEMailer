package mq

import java.util.UUID

import akka.actor.{ActorRef, TypedActor}
import com.typesafe.config.Config
import mq.RMQProtocol._
import org.slf4j.LoggerFactory

/**
  * Actor exchange 'protocol'
  * Source message with ack tag and content RMQMessage(tag: Long, message: String)
  * Reply message with ack tag AckMessage(tag: Long)
  */
object RMQProtocol {
  case class RMQMessage(tag: Long, message: String)
  case class AckMessage(tag: Long)
  case class AckMessageT(id: UUID)
}

trait RMQHandlerT {
  def consume(tag: Long, msg: Array[Byte]): Unit
  def ack(tag: Long): Unit
  def close(): Unit
}

/**
  * Run RMQ consumer and read messages from Rabbit Queue
  */
class RMQHandler(conf: Config, owner: ActorRef) extends RMQHandlerT {
  private val log = LoggerFactory.getLogger(classOf[RMQHandler])
  private val rmq = new RMQConnector(conf, consume)
  private val self = TypedActor.get(TypedActor.context).getActorRefFor(TypedActor.self)

  /**
    * Pass Ack tag back to RMQ
    * @param tag
    */
  override def ack(tag: Long): Unit = {
    log.debug(s"Ack: $tag")
    rmq.ack(tag)
  }

  /**
    * Consume RMQ message and send to owner (RMQActor)
    * @param tag RMQ message tag
    * @param msg message body
    */
  override def consume(tag: Long, msg: Array[Byte]): Unit = {
    val message = RMQMessage(tag, new String(msg))
    log.debug(s"Consumed message: $message")
    owner.tell(message, self)
  }

  /**
    * Close RMQ Connection
    */
  override def close(): Unit = rmq.close()
}
