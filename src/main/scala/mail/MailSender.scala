package mail

import java.util.Properties
import javax.mail.Message

import com.typesafe.config.Config
import org.slf4j.LoggerFactory
import javax.mail.Session
import util.LoanPattern.using

/**
  * Send email message
  *
  * @param config Configuration
  */
class MailSender(config: Config) {
  private val log = LoggerFactory.getLogger(classOf[MailSender])

  def send(message: Message): Unit = send(message, createSession)

  def send(message: Message, session: Session): Unit = {
    val host = config.getString("mail.server.host")
    val port = config.getInt("mail.server.port")
    val user = config.getString("mail.server.user")
    val pass = config.getString("mail.server.password")

    session.setDebug(config.getBoolean("mail.server.debug"))
    log.trace("Obtaining transport")
    using(session.getTransport(config.getString("mail.server.protocol"))) { transport =>
      log.debug("Transport received. Connecting {}:{}", host, port)
      if (port > 0) {
        transport.connect(host, port, user, pass)
      } else {
        transport.connect(host, user, pass)
      }
      log.debug("Host connected. Sending message")
      transport.sendMessage(message, message.getAllRecipients)
      log.trace("Message sent. Closing transport")
    }
  }

  private def createSession: Session = createSession(System.getProperties)

  private def createSession(props: Properties): Session = {
    log.trace("Obtaining session")
    if (config.getBoolean("mail.server.auth")) {
      log.debug("Setting smtp auth: mail.smtps.auth=true")
      props.put("mail.smtps.auth", "true")
    }
    Session.getInstance(props)
  }
}
