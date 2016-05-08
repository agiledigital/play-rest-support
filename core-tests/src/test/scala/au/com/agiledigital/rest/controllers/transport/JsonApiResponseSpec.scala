package au.com.agiledigital.rest.controllers.transport

import au.com.agiledigital.rest.tests.BaseSpec
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.DataTables
import play.api.libs.json.Json

/**
  * Tests for [[JsonApiResponse]].
  */
class JsonApiResponseSpec(implicit ev: ExecutionEnv) extends BaseSpec with DataTables {
  "JsonApiResponse writes" should {
    // format: OFF
     "remove unsafe HTML tags from JSON API responses" ||
       "description"                             || "input string"                      || "expected"                                                 |>
       "leave non-HTML strings alone"            !! "This is a string"                  !! "This is a string"                                         |
       "remove unsafe attribute from the string" !! "<p style=\"display: none\"></p>"   !! "<p></p>"                                                  |
       "remove unsafe tags from the string"      !! "<img>unsafe tags<scripts>"         !! "unsafe tags"                                              |
       "allow heading for the string"            !! "<h1></h1>"                         !! "<h1></h1>"                                                |
       "remove DIV tag"                          !! "<div>This is a div</div>"          !! "This is a div"                                            |
       "allow anchor attributes"                 !! "<a title=\"test\" target=\"new\">" !! "<a title=\"test\" target=\"new\" rel=\"nofollow\"></a>"   |>{
       (description, inputString, expected) => {
            // format: ON

            // Use inputString as the value, the message and the context to ensure all three have HTML filtering applied.
            val response = JsonApiResponse(Some(inputString), Message(inputString, MessageLevel.Info, Json.obj("foo" -> inputString)))

            val json = Json.toJson(response)
            json must beEqualTo(Json.obj("result" -> expected, "messages" -> Seq(Json.obj("message" -> expected, "level" -> "Info", "context" -> Json.obj("foo" -> expected)))))
          }
      }
  }

  "Adding messages" should {
    "add a new message" in {
      // Given a JsonApiResponse
      val response = JsonApiResponse(Some("result"), Message("original message"))

      // When a new message is added.
      val addedMessage = Message(
        message = "new message",
        level = MessageLevel.Alert,
        code = Some(99)
      )
      val updatedResponse = response.withMessage(addedMessage)

      // Then the updated response should include both messages.
      updatedResponse must_=== JsonApiResponse(
        Some("result"),
        Seq(
          Message("original message"),
          addedMessage
        )
      )
    }

  }
}