package model

import java.util.UUID

import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._

/**
  * Parsed command (message)
  *  {
  *    "id": "35ec8679-d158-4761-9f94-f0c438ad6f78",
  *    "type": "welcome",
  *    "recipient": "obi-wan@rootservices.org",
  *    "first_name": "obi-wan",
  *    "target": "https://rootservices.org/verify"
  *  }
  *  Possible types: "welcome" or "reset_password"
  */
object RequestProtocol {
  import model.SimpleRequestProtocol._

  case class RequestMessage(id: UUID, rtype: String, recipient: String, firstName: String, target: String)

  implicit val requestMessageReads: Reads[RequestMessage] = (
    (JsPath \ 'id).read[String].map(UUID.fromString) and
      (JsPath \ 'type).read[String] and
      (JsPath \ 'recipient).read[String] and
      (JsPath \ 'first_name).read[String] and
      (JsPath \ 'target).read[String]
    ) (RequestMessage.apply _)

  implicit class RequestMessageOps(msg: RequestMessage) {
    def toRequest: RequestT = {
      if ("welcome".equals(msg.rtype)) {
        Welcome(id = msg.id, recipient = msg.recipient, firstName = msg.firstName, target = msg.target)
      } else {
        ResetPassword(id = msg.id, recipient = msg.recipient, firstName = msg.firstName, target = msg.target)
      }
    }
  }

}

object SimpleRequestProtocol {
  import model.RequestProtocol.RequestMessage

  trait RequestT {
    def id: UUID
    def recipient: String
    def firstName: String
    def target: String
  }

  case class Welcome(id: UUID, recipient: String, firstName: String, target: String) extends RequestT

  case class ResetPassword(id: UUID, recipient: String, firstName: String, target: String) extends RequestT

  implicit class RequestOps(msg: RequestT) {
    def toRequestMessage: RequestMessage = {
      RequestMessage(
        id = msg.id,
        recipient = msg.recipient,
        firstName = msg.firstName,
        target = msg.target,
        rtype = msg match {
          case _: Welcome => "welcome"
          case _: ResetPassword => "reset_password"
        }
      )
    }
  }
}
