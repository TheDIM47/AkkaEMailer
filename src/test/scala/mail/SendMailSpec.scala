package mail

import com.icegreen.greenmail.util._
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}

import scala.util.Try

class SendMailSpec extends FlatSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  val protocols = Array[ServerSetup](ServerSetupTest.SMTPS, ServerSetupTest.SMTP)
  var smtp: GreenMail = _

  "GreenMail" should "send email to mail server with smtp(s)" in {
    smtp.setUser("test@email.com", "user", "pass")
    GreenMailUtil.sendTextEmailTest("to@host.com", "from@host.com", "subject - 1", "body - 1")
    GreenMailUtil.sendTextEmailSecureTest("abc@host.com", "zero@host.com", "subject - 2", "body - 2")
    smtp.getReceivedMessages.length shouldBe 2
  }

  "Mail Sender" should "send email to smtp" in {
    val conf = ConfigFactory.load("application-test.conf")
    val sender = new MailSender(conf)
    val message = new EmailBuilder()
      .from("source@email.com")
      .to("target@email.com")
      .subj("test")
      .withHtml("<h1>Hello</h1>")
      .build()
    smtp.setUser("admin@email.com", conf.getString("mail.server.user"), conf.getString("mail.server.password"))
    sender.send(message)
    smtp.waitForIncomingEmail(1000, 1)
    smtp.getReceivedMessages.length shouldBe 1
  }

  it should "fail on invalid server" in {
    val conf = ConfigFactory.load("application.conf")
    val sender = new MailSender(conf)
    val message = new EmailBuilder()
      .from("source@email.com")
      .to("target@email.com")
      .subj("failure test")
      .withText("<h1>Fail!</h1>")
      .build()
    an [com.sun.mail.util.MailConnectException] should be thrownBy {
      sender.send(message)
    }
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
