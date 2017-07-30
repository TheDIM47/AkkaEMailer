package mail

import java.util.UUID

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.{JsError, JsResult, JsSuccess, Json}

class RequestMessageSpec extends FlatSpec with Matchers {

  import model.RequestProtocol._

  "Message reader" should "read json message" in {
    val sample =
      """
        |{
        |  "id": "35ec8679-d158-4761-9f94-f0c438ad6f78",
        |  "type": "welcome",
        |  "recipient": "obi-wan@rootservices.org",
        |  "first_name": "obi-wan",
        |  "target": "https://rootservices.org/verify"
        |}
      """.stripMargin

    val value: JsResult[RequestMessage] = Json.parse(sample).validate[RequestMessage]
    value match {
      case JsSuccess(req, _) =>
        println(req)
        req.id shouldBe UUID.fromString("35ec8679-d158-4761-9f94-f0c438ad6f78")
        req.rtype shouldBe "welcome"
        req.recipient shouldBe "obi-wan@rootservices.org"
        req.firstName shouldBe "obi-wan"
        req.target shouldBe "https://rootservices.org/verify"
      case JsError(err) =>
        fail(err.toString)
    }
  }

  it should "fail on incomplete message" in {
    val sample =
      """
        |{
        |  "id": "35ec8679-d158-4761-9f94-f0c438ad6f78",
        |  "recipient": "obi-wan@rootservices.org",
        |  "first_name": "obi-wan",
        |  "target": "https://rootservices.org/verify"
        |}
      """.stripMargin
    val value: JsResult[RequestMessage] = Json.parse(sample).validate[RequestMessage]
    value shouldBe a [JsError]
  }
}
