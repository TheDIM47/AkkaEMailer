package flow

import javax.mail.MessagingException

import akka.actor.SupervisorStrategy._
import akka.actor.{ActorSystem, OneForOneStrategy, Props}
import akka.testkit.{TestActorRef, TestKit, TestProbe}

import scala.concurrent.duration._
import com.icegreen.greenmail.util.{GreenMail, ServerSetup, ServerSetupTest}
import com.typesafe.config.ConfigFactory
import mail.{MailerActor, MessageSenderImpl}
import mq.RMQProtocol.{AckMessage, RMQMessage}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpecLike, Matchers}
import org.slf4j.LoggerFactory

import scala.util.Try

class MailerActorSpec extends TestKit(ActorSystem("SendMailActorSpec")) with FlatSpecLike with Matchers
  with BeforeAndAfterAll with BeforeAndAfterEach {

  val log = LoggerFactory.getLogger(classOf[MailerActorSpec])

  val conf = ConfigFactory.load("application-test.conf")
  val protocols = Array[ServerSetup](ServerSetupTest.SMTPS, ServerSetupTest.SMTP)
  var smtp: GreenMail = _

  val rnd = scala.util.Random
  val validMessage = RMQMessage(rnd.nextInt(1000).toLong,
      """{ "id": "35ec8679-d158-4761-9f94-f0c438ad6f78", "recipient": "obi-wan@rootservices.org",
        |  "type": "welcome", "first_name": "obi-wan",
        |  "target": "https://rootservices.org/verify" }""".stripMargin)
  val invalidMessage = RMQMessage(rnd.nextInt(1000).toLong,
      """{ "id": "35ec8679-d158-4761-9f94-f0c438ad6f78", "recipient": "obi-wan@rootservices.org",
        |  "target": "https://rootservices.org/verify" }""".stripMargin)

  "Send mail actor" should "send email on valid message" in {
    val validProps = Props(new MailerActor(conf) with MessageSenderImpl)
    val probe = TestProbe()
    val mailer = system.actorOf(validProps)
    probe.watch(mailer)
    probe.send(mailer, validMessage)
    smtp.waitForIncomingEmail(1000, 1)
    smtp.getReceivedMessages.length shouldBe 1
    probe.expectMsg(AckMessage(validMessage.tag))
    probe.expectTerminated(mailer, 3.seconds)
  }

  it should "fail with Ack on invalid message" in {
    val validProps = Props(new MailerActor(conf) with MessageSenderImpl)
    val probe = TestProbe()
    val mailer = system.actorOf(validProps)
    probe.watch(mailer)
    probe.send(mailer, invalidMessage)
    probe.expectMsg(AckMessage(invalidMessage.tag))
    probe.expectTerminated(mailer, 3.seconds)
    smtp.waitForIncomingEmail(250, 1)
    smtp.getReceivedMessages.length shouldBe 0
  }

  it should "fail on invalid connection" in {
    smtp.stop()
    val mailer = TestActorRef(new MailerActor(conf) with MessageSenderImpl)
    intercept[com.sun.mail.util.MailConnectException] {
      mailer.receive(validMessage)
    }
    smtp.start()
  }

  it should "fail and restart" in {
    val probe = TestProbe()
    val validProps = Props(new MailerActor(conf) with MessageSenderImpl)
    val ss = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
      case _: MessagingException => Restart
      case _ => Stop
    }
    val mailer = probe.childActorOf(validProps, ss)
    probe.watch(mailer)

    smtp.stop()
    probe.send(mailer, validMessage)
    probe.expectNoMsg(250.millis)

    smtp.start()
    probe.send(mailer, validMessage)

    probe.expectMsg(AckMessage(validMessage.tag))
    probe.expectTerminated(mailer, 3.seconds)
    smtp.waitForIncomingEmail(250, 1)
    smtp.getReceivedMessages.length shouldBe 1
  }

  override protected def beforeAll(): Unit = {
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
  }

  override protected def beforeEach(): Unit = {
    smtp.purgeEmailFromAllMailboxes()
  }

  override protected def afterEach(): Unit = {
    smtp.purgeEmailFromAllMailboxes()
  }
}
