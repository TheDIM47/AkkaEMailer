package mq

import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client._
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

/**
  * RMQ Message Consumer (basicConsume(...) Callback)
  *
  * @param channel RMQ Channel
  * @param consumer Handle received RMQ Message
  */
class RMQConsumer[A](channel: Channel, consumer: (Long, Array[Byte]) => A) extends DefaultConsumer(channel: Channel) {
  private val log = LoggerFactory.getLogger(classOf[RMQConsumer[A]])

  override def handleDelivery(consumerTag: String, envelope: Envelope, properties: BasicProperties, body: Array[Byte]): Unit = {
    log.debug(s"Got properties: $properties, message: ${new String(body)}")
    consumer.apply(envelope.getDeliveryTag, body)
  }

  override def handleShutdownSignal(consumerTag: String, sig: ShutdownSignalException): Unit = {
    log.info(s"Received shutdown signal: $sig")
  }

  override def handleRecoverOk(consumerTag: String): Unit = {
    log.info(s"Recovering completed")
  }
}

/**
  * RMQ Connector with Connection auto-recovering
  *
  * @param conf RabbitMQ configuration section
  * @param consumer consumer function
  * @tparam A function result type
  */
class RMQConnector[A](conf: Config, consumer: (Long, Array[Byte]) => A) extends AutoCloseable {
  import scala.collection.JavaConverters._

  private val log = LoggerFactory.getLogger(this.getClass)
  private val factory = createFactory(conf)
  private val connection: Connection = factory.newConnection()
  private val channel: Channel = connection.createChannel()
  private val exchangeName = conf.getString("source.rabbitmq.exchange")
  private val routingKey = conf.getString("source.rabbitmq.routing-key")

  channel.exchangeDeclare(exchangeName, "topic", true)
  private val queueName = channel.queueDeclare(exchangeName, true, false, false, null).getQueue
  log.debug(s"Exchane: $exchangeName Queue: $queueName")
  channel.queueBind(queueName, exchangeName, routingKey)

  private val rmqConsumer = new RMQConsumer(channel, consumer)
//  val props = Map("content-type" -> "application/json".asInstanceOf[Object]).asJava
//  val props2: BasicProperties = new AMQP.BasicProperties.Builder().contentType("application/json").build()
  channel.basicConsume(queueName, false, Map.empty[String, AnyRef].asJava, rmqConsumer)

  def ack(tag: Long): Unit = {
    channel.basicAck(tag, false)
  }

  override def close(): Unit = {
    channel.close()
    connection.close()
  }

  private def createFactory(conf: Config): ConnectionFactory = {
    val factory = new ConnectionFactory()
    factory.setHost(conf.getString("source.rabbitmq.host"))
    factory.setPort(conf.getInt("source.rabbitmq.port"))
    factory.setUsername(conf.getString("source.rabbitmq.login"))
    factory.setPassword(conf.getString("source.rabbitmq.password"))

    factory.setAutomaticRecoveryEnabled(conf.getBoolean("source.rabbitmq.automatic-recovery"))
    factory.setNetworkRecoveryInterval(conf.getInt("source.rabbitmq.network-recovery-interval"))
    factory.setTopologyRecoveryEnabled(conf.getBoolean("source.rabbitmq.topology-recovery"))

    factory.setRequestedHeartbeat(conf.getInt("source.rabbitmq.requested-heartbeat"))
    factory.setConnectionTimeout(conf.getInt("source.rabbitmq.connection-timeout"))
    factory
  }
}
