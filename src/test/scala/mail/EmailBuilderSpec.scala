package mail

import javax.mail.Message.RecipientType
import javax.mail.internet.{InternetAddress, MimeMultipart}

import org.scalatest.{FlatSpec, Matchers}

class EmailBuilderSpec extends FlatSpec with Matchers {

  "Email builder" should "build message" in {
    val eb = new EmailBuilder()
          .from("a@b.c")
          .replyTo("e@f.g")
          .to("c@b.a,d@e.f")
          .cc("")
          .subj("test")
          .withText("Hello")
          .withText("Hello world")

    val msg = eb.build()
    msg.getSubject shouldBe "test"
    msg.getFrom shouldBe InternetAddress.parse("a@b.c")
    msg.getReplyTo shouldBe InternetAddress.parse("e@f.g")

    msg.getRecipients(RecipientType.TO) shouldBe InternetAddress.parse("c@b.a,d@e.f")
    msg.getRecipients(RecipientType.BCC) shouldBe null
    msg.getRecipients(RecipientType.CC) shouldBe null

    msg.getContent.asInstanceOf[MimeMultipart]
      .getBodyPart(0).getContent shouldBe "Hello world"
  }

}
