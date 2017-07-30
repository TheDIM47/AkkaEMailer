package rep

import java.util.UUID

import com.typesafe.config.ConfigFactory
import mail.ReportBuilder
import model.RequestProtocol.RequestMessage
import org.scalatest.{FlatSpec, Matchers}

class ReportSpec extends FlatSpec with Matchers {
  val conf = ConfigFactory.load("application-test.conf")

  "Report" should "render welcome message" in {
    val rb = new ReportBuilder(conf)
    val msg = RequestMessage(UUID.randomUUID(), "welcome", "John@Doe.com", "John Doe", "http://goo.gl")
    val rep = rb.build(msg)
    println(rep)
    rep should include ("John Doe")
    rep should include ("<a href=\"http://goo.gl\">")
  }

  it should "render reset message" in {
    val rb = new ReportBuilder(conf)
    val msg = RequestMessage(UUID.randomUUID(), "reset_password", "Sara@Doe.com", "Sara Doe", "http://bit.ly")
    val rep = rb.build(msg)
    println(rep)
    rep should include ("Sara Doe")
    rep should include ("<a href=\"http://bit.ly\">")
  }

  it should "fail on invalid template type" in {
    val rb = new ReportBuilder(conf)
    val msg = RequestMessage(UUID.randomUUID(), "reset_welcome", "Sara@Doe.com", "Sara Doe", "http://bit.ly")
    intercept[com.typesafe.config.ConfigException] {
      rb.build(msg)
    }
  }
}
