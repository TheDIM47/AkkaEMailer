package mail

import com.typesafe.config.Config
import model.RequestProtocol.RequestMessage
import model.SimpleRequestProtocol._
import org.clapper.scalasti.STGroupFile
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}

/**
  * Load and render report template
  *
  * @param conf Configuration
  */
class ReportBuilder(conf: Config) {
  private val log = LoggerFactory.getLogger(classOf[ReportBuilder])

  lazy val group = STGroupFile(conf.getString("report.file"))

  def build(message: RequestMessage): String = build(conf.getString(s"report.${message.rtype}"), message)

  def build(message: RequestT): String = {
    val msg = message.toRequestMessage
    build(conf.getString(s"report.${msg.rtype}"), msg)
  }

  // Template indexed by templateName. See application.conf
  private def build(templateName: String, message: RequestMessage): String = {
    group.instanceOf(templateName) match {
      case Success(template) =>
        template
          .add("id", message.id)
          .add("type", message.rtype)
          .add("recipient", message.recipient)
          .add("first_name", message.firstName)
          .add("target", message.target)
          .render() match {
          case Success(result) => result
          case Failure(ex) =>
            log.error(s"Unable to render template $templateName, ${ex.getMessage}")
            throw ex
        }
      case Failure(ex) =>
        log.error(s"Unable to load template $templateName, ${ex.getMessage}")
        throw ex
    }
  }
}
