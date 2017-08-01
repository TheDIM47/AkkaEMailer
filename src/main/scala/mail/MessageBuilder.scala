package mail

import java.io.File
import java.util.Properties
import javax.mail.{Address, Message, Session}
import javax.mail.internet._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Simple Email builder
  */
class EmailBuilder {
  private var from: Option[InternetAddress] = None
  private var reply: Array[InternetAddress] = Array[InternetAddress]()
  private val to = ArrayBuffer[InternetAddress]()
  private val cc = ArrayBuffer[InternetAddress]()
  private val bcc = ArrayBuffer[InternetAddress]()
  private var subj: String = ""
  private var text: Option[String] = None
  private var content: Option[(String, String)] = None

  //  private var alternatives = mutable.ArrayBuffer[MimeBodyPart]()
  private val attaches = mutable.ArrayBuffer[MimeBodyPart]()

  def from(f: String): EmailBuilder = {
    val addresses: Array[InternetAddress] = InternetAddress.parse(f)
    if (addresses.length > 0) from = Some(addresses(0))
    this
  }

  def replyTo(r: String): EmailBuilder = {
    reply = InternetAddress.parse(r)
    this
  }

  def to(t: String): EmailBuilder = {
    to ++= InternetAddress.parse(t)
    this
  }

  def cc(t: String): EmailBuilder = {
    cc ++= InternetAddress.parse(t)
    this
  }

  def bcc(t: String): EmailBuilder = {
    bcc ++= InternetAddress.parse(t)
    this
  }

  def subj(s: String): EmailBuilder = {
    subj = s
    this
  }

  def withText(text: String): EmailBuilder = {
    this.text = Some(text)
    this
  }

  def withHtml(content: String): EmailBuilder = withContent(content, "text/html; charset=UTF-8")

  def withContent(content: String, contentType: String): EmailBuilder = {
    this.content = Some((content, contentType))
    this
  }

  // for attached files
  def attach(f: File): EmailBuilder = {
    val filePart = new MimeBodyPart()
    filePart.attachFile(f)
    attaches += filePart
    this
  }

  // for embedded images
  def embed(f: File, id: String): EmailBuilder = {
    val imagePart = new MimeBodyPart()
    imagePart.attachFile(f)
    imagePart.setContentID(id)
    attaches += imagePart
    this
  }

  private def build(parts: mutable.ArrayBuffer[MimeBodyPart], pure: MimeMultipart = new MimeMultipart()): MimeMultipart = {
    parts.foldLeft(pure)((m, bp) => {
      m.addBodyPart(bp)
      m
    })
  }

  def build(): MimeMessage = build(new MimeMessage(Session.getDefaultInstance(new Properties())))

  def build(message: MimeMessage): MimeMessage = {
    val multipart = new MimeMultipart()

    val textPart = text.map(t => {
      val p = new MimeBodyPart()
      p.setText(t)
      p
    })
    val htmlPart = content.map(t => {
      val p = new MimeBodyPart()
      p.setContent(t._1, t._2)
      p
    })
    val alt = if (textPart.isDefined && htmlPart.isDefined) new MimeMultipart("alt") else multipart
    textPart.foreach(bp => alt.addBodyPart(bp))
    htmlPart.foreach(bp => alt.addBodyPart(bp))
    if (alt != multipart) {
      val bp = new MimeBodyPart()
      bp.setContent(alt)
      multipart.addBodyPart(bp)
    }

    val atts: MimeMultipart = build(attaches)
    if (atts.getCount > 0) {
      val w = new MimeBodyPart()
      w.setContent(atts)
      multipart.addBodyPart(w)
    }

    from.foreach(message.setFrom)
    if (reply.nonEmpty) message.setReplyTo(reply.map(_.asInstanceOf[Address]))
    if (to.nonEmpty) message.setRecipients(Message.RecipientType.TO, to.toArray[Address])
    if (cc.nonEmpty) message.setRecipients(Message.RecipientType.CC, cc.toArray[Address])
    if (bcc.nonEmpty) message.setRecipients(Message.RecipientType.BCC, bcc.toArray[Address])

    message.setSubject(subj)
    message.setContent(multipart)

    message
  }
}
