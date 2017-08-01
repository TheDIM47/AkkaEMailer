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

  case class RequestMessage(id: UUID, rtype: String, recipient: String, firstName: String, target: String)

  implicit val requestMessageReads: Reads[RequestMessage] = (
    (JsPath \ 'id).read[String].map(UUID.fromString) and
      (JsPath \ 'type).read[String] and
      (JsPath \ 'recipient).read[String] and
      (JsPath \ 'first_name).read[String] and
      (JsPath \ 'target).read[String]
    ) (RequestMessage.apply _)

}
