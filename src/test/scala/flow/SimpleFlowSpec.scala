package flow

import java.util.UUID

import akka.actor.{ActorSystem, PoisonPill}
import akka.testkit.TestKit
import com.icegreen.greenmail.util.{GreenMail, ServerSetup, ServerSetupTest}
import com.typesafe.config.ConfigFactory
import mail.MessageSupervisor
import model.SimpleRequestProtocol._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpecLike, Matchers}
import org.slf4j.LoggerFactory

import scala.util.Try

class SimpleFlowSpec extends TestKit(ActorSystem("SimpleSendMailActorSpec")) with FlatSpecLike with Matchers
  with BeforeAndAfterAll with BeforeAndAfterEach {

  val log = LoggerFactory.getLogger(classOf[MailerActorSpec])

  val conf = ConfigFactory.load("application-test.conf")
  val protocols = Array[ServerSetup](ServerSetupTest.SMTPS, ServerSetupTest.SMTP)
  var smtp: GreenMail = _

  val rnd = scala.util.Random
  val msgWelcome = Welcome(UUID.randomUUID, "obi-wan@rootservices.org", "obi-wan", "https://rootservices.org/verify")
  val msgReset = ResetPassword(UUID.randomUUID, "obi-wan@rootservices.org", "obi-wan", "https://rootservices.org/verify")

  "MessageSupervisor" should "send welcome and reset messages" in {
    val sv = system.actorOf(MessageSupervisor.props(conf), name = "supervisor-1")

    sv ! msgWelcome
    smtp.waitForIncomingEmail(1000, 1)
    smtp.getReceivedMessages.length shouldBe 1

    sv ! msgReset
    smtp.waitForIncomingEmail(1000, 2)
    smtp.getReceivedMessages.length shouldBe 2

    sv ! PoisonPill
  }

  it should "send message after mail server restarted" in {
    val sv = system.actorOf(MessageSupervisor.props(conf), name = "supervisor-1")

    smtp.stop()
    sv ! msgWelcome
    smtp.waitForIncomingEmail(1000, 0)
    smtp.getReceivedMessages.length shouldBe 0

    smtp.start()
    smtp.waitForIncomingEmail(1000, 1)
    smtp.getReceivedMessages.length shouldBe 1

    sv ! PoisonPill
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
