package flow

import akka.actor.{ActorSystem, PoisonPill}
import akka.testkit.{TestKit, TestProbe}
import com.icegreen.greenmail.util.{GreenMail, ServerSetup, ServerSetupTest}
import com.typesafe.config.ConfigFactory
import io.arivera.oss.embedded.rabbitmq.{EmbeddedRabbitMq, EmbeddedRabbitMqConfig}
import mq.RMQActor
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client._
import org.scalatest._
import org.slf4j.LoggerFactory

import scala.util.Try

class RmqFlowSpec extends TestKit(ActorSystem("SendMailActorSpec")) with FlatSpecLike with Matchers
  with BeforeAndAfterAll with BeforeAndAfterEach {

  val log = LoggerFactory.getLogger(classOf[RmqFlowSpec])

  val conf = ConfigFactory.load("application-test.conf")
  val protocols = Array[ServerSetup](ServerSetupTest.SMTPS, ServerSetupTest.SMTP)

  var smtp: GreenMail = _
  var rabbitMq: EmbeddedRabbitMq = _

  val validMessage =
    """{
      |  "id": "35ec8679-d158-4761-9f94-f0c438ad6f78",
      |  "recipient": "obi-wan@rootservices.org",
      |  "type": "welcome",
      |  "first_name": "obi-wan",
      |  "target": "https://rootservices.org/verify"
      |}""".stripMargin

  "Application" should "send several valid messages" in {
    val listener = system.actorOf(RMQActor.props(conf), name = "rmq-listener-1")

    sendRmqMessage(validMessage)
    smtp.waitForIncomingEmail(1000, 1)
    smtp.getReceivedMessages.length shouldBe 1

    sendRmqMessage(validMessage)
    smtp.waitForIncomingEmail(1000, 2)
    smtp.getReceivedMessages.length shouldBe 2

    sendRmqMessage(validMessage)
    smtp.waitForIncomingEmail(1000, 3)
    smtp.getReceivedMessages.length shouldBe 3

    listener ! PoisonPill
  }

  /*
  it should "send valid message even smtp was not available" in {
    val listener = system.actorOf(RMQActor.props(conf), name = "rmq-listener-2")

    smtp.stop()
    sendRmqMessage(validMessage)
    smtp.waitForIncomingEmail(1000, 0)
    smtp.getReceivedMessages.length shouldBe 0

    smtp.start()
    smtp.waitForIncomingEmail(1000, 1)
    smtp.getReceivedMessages.length shouldBe 1

    listener ! PoisonPill
  }
  */

  private def sendRmqMessage(message: String): Unit = {
    val factory = new ConnectionFactory()
    factory.setHost(conf.getString("source.rabbitmq.host"))
    factory.setPort(conf.getInt("source.rabbitmq.port"))
    factory.setUsername(conf.getString("source.rabbitmq.login"))
    factory.setPassword(conf.getString("source.rabbitmq.password"))
    val exchangeName = conf.getString("source.rabbitmq.exchange")
    val routingKey = conf.getString("source.rabbitmq.routing-key")

    val connection: Connection = factory.newConnection()
    val channel: Channel = connection.createChannel()

    channel.exchangeDeclare(exchangeName, "topic", true)
    val queueName = channel.queueDeclare(exchangeName, true, false, false, null).getQueue
    log.debug(s"Exchane: $exchangeName Queue: $queueName")
    channel.queueBind(queueName, exchangeName, routingKey)

    channel.basicPublish(exchangeName, routingKey, new BasicProperties.Builder().build(), message.getBytes)
  }

  override protected def beforeAll(): Unit = {
    val config: EmbeddedRabbitMqConfig = new EmbeddedRabbitMqConfig.Builder().build()
    rabbitMq = new EmbeddedRabbitMq(config)
    rabbitMq.start()

    smtp = new GreenMail(protocols)
    Try(smtp.start()).recover({
      case _ =>
        Thread.sleep(5000)
        smtp.start()
    })
  }

  override protected def afterAll(): Unit = {
    smtp.stop()
    smtp = null

    rabbitMq.stop()
    rabbitMq = null
  }

  override protected def beforeEach(): Unit = {
    smtp.purgeEmailFromAllMailboxes()
  }

  override protected def afterEach(): Unit = {
    smtp.purgeEmailFromAllMailboxes()
  }
}
