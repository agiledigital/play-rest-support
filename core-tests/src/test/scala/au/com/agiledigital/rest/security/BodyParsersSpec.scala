package au.com.agiledigital.rest.security

import au.com.agiledigital.rest.tests.BaseSpec
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.DataTables
import play.api.Application
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.mvc.Results._
import play.api.mvc.{ Action, ControllerComponents }
import play.api.test.FakeRequest
import play.api.test.WithApplication

/**
  * Tests for [[BodyParsers]].
  */
class BodyParsersSpec(implicit ev: ExecutionEnv) extends BaseSpec with DataTables {
  "BodyParsers clean" should {

    val jsString = JsString("<img src=\"foo\"><span>bar</span><script>alert('baz');</script> baz")

    val jsArray = Json.arr(Json.arr("<img src=\"foo\"><span>bar</span><script>alert('baz');</script> baz"))

    val expectedArray = Json.arr(Json.arr("bar baz"))

    val expectedUnsafeArray = Json.arr(Json.arr("<span>bar</span> baz"))

    val jsObject = Json.obj(
      "foo" -> Json.obj(
        "bar" -> "<img src=\"foo\"><span>bar</span><script>alert('baz');</script> baz"
      )
    )

    val expectedObject = Json.obj(
      "foo" -> Json.obj(
        "bar" -> "bar baz",
        "barUnsafe" -> "<img src=\"foo\"><span>bar</span><script>alert('baz');</script> baz"
      ),
      "fooUnsafe" -> Json.obj(
        "bar" -> "<img src=\"foo\"><span>bar</span><script>alert('baz');</script> baz"
      )
    )

    val expectedUnsafeObject = Json.obj(
      "foo" -> Json.obj(
        "bar" -> "<span>bar</span> baz",
        "barUnsafe" -> "<img src=\"foo\"><span>bar</span><script>alert('baz');</script> baz"
      ),
      "fooUnsafe" -> Json.obj(
        "bar" -> "<img src=\"foo\"><span>bar</span><script>alert('baz');</script> baz"
      )
    )

    // format: OFF
    "clean JSON values using the specified HTML whitelist" in new WithApplication {
      "description"                       || "json"               || "expected clean all" || "expected clean unsafe"          |>
      "leave non-HTML strings alone"      !! JsString("bar baz")  !! JsString("bar baz")  !! JsString("bar baz")              |
      "remove HTML elements"              !! jsString             !! JsString("bar baz")  !! JsString("<span>bar</span> baz") |
      "apply recursively to JSON arrays"  !! jsArray              !! expectedArray        !! expectedUnsafeArray              |
      "apply recursively to JSON objects" !! jsObject             !! expectedObject       !! expectedUnsafeObject             |
      "leave JSON numbers alone"          !! JsNumber(42)         !! JsNumber(42)         !! JsNumber(42)                     |
      "leave JSON null alone"             !! JsNull               !! JsNull               !! JsNull                           |
      "leave JSON bools alone"            !! JsBoolean(false)     !! JsBoolean(false)     !! JsBoolean(false)                 |> {
      (description, json, expectedCleanAll, expectedUnsafe) => {
              // format: ON
              val bodyParsers = new BodyParsers {
                override protected def controllerComponents: ControllerComponents = app.injector.instanceOf[ControllerComponents]
              }

              bodyParsers.clean(json) must beEqualTo(expectedCleanAll)

              bodyParsers.cleanUnsafe(json) must beEqualTo(expectedUnsafe)
            }
        }
    }
  }

  "Parsing JSON to value with specific type after applying a full clean white listing filter" should {

    // Given a fake action that uses the whitelistingJson body parser with type MockTestModel.
    def fakeAction(implicit app: Application): Action[MockTestModel] = {
      val controller = new BodyParsers {
        override protected def controllerComponents: ControllerComponents = app.injector.instanceOf[ControllerComponents]
      }

      controller.Action(controller.whitelistingJson[MockTestModel]) { request =>
        Ok(Json.toJson(request.body))
      }
    }

    "clean the JSON values and validate the JsValue based on the type" in new WithApplication {
      // And a request with the JSON body match the MockTestModel with HTML tags.
      val request = FakeRequest().withBody(Json.parse("""{"id": 1, "name": "<script>Hack</script><span>safe</span>"}"""))

      // When calling the fake action with the request.
      val result = call(fakeAction, request)

      // Then the response content must have the JSON body cleaned.
      contentAsString(result)(defaultTimeout) must_=== """{"id":1,"name":"safe"}"""

      // And the status is 200 OK.
      status(result)(defaultTimeout) must_=== OK
    }

    "return Bad Request if the request body contains invalid JSON" in new WithApplication {
      // And a request with the JSON body with invalid JSON.
      val request = FakeRequest().withBody(Json.parse("""{"id": 1}"""))

      // When calling the fake action with the request.
      val result = call(fakeAction, request)

      // Then the status is 400 Bad Request.
      status(result)(defaultTimeout) must_=== BAD_REQUEST
    }
  }

  "Applying a full clean white listing filter to a JSON body" should {

    // Given a fake action that uses the whitelistingJson body parser.
    def fakeAction(implicit app: Application): Action[JsValue] = {
      val controller = new BodyParsers {
        override protected def controllerComponents: ControllerComponents = app.injector.instanceOf[ControllerComponents]
      }

      controller.Action(controller.whitelistingJson) { request =>
        Ok(request.body)
      }
    }

    "clean the JSON values" in new WithApplication {
      // And a request with a JSON body with HTML tags.
      val request = FakeRequest().withBody(Json.parse("""{"name": "<script>Hack</script><span>safe</span>"}"""))

      // When calling the fake action with the request.
      val result = call(fakeAction, request)

      // Then the response content must have the JSON body cleaned.
      contentAsString(result)(defaultTimeout) must_=== """{"name":"safe","nameUnsafe":"<script>Hack</script><span>safe</span>"}"""

      // And the status is 200 OK.
      status(result)(defaultTimeout) must_=== OK
    }
  }

  "Parsing JSON to value with specific type after applying an unsafe white listing filter" should {

    // Given a fake action that uses the whitelistingJsonUnSafe body parser.
    def fakeAction(implicit app: Application): Action[MockTestModel] = {
      val controller = new BodyParsers {
        override protected def controllerComponents: ControllerComponents = app.injector.instanceOf[ControllerComponents]
      }

      controller.Action(controller.whitelistingJsonUnsafe[MockTestModel]) { request =>
        Ok(Json.toJson(request.body))
      }
    }

    "clean the JSON values with safe HTML tags remain and validate the JsValue based on the type" in new WithApplication {
      // And a request with the JSON body match the MockTestModel with HTML tags.
      val request = FakeRequest().withBody(Json.parse("""{"id": 1, "name": "<script>Hack</script><span>safe</span>"}"""))

      // When calling the fake action with the request.
      val result = call(fakeAction, request)

      // Then the response content must have the JSON body's unsafe HTML tags cleaned.
      contentAsString(result)(defaultTimeout) must_=== """{"id":1,"name":"<span>safe</span>"}"""

      // And the status is 200 OK.
      status(result)(defaultTimeout) must_=== OK
    }

    "return Bad Request if the request body contains invalid JSON" in new WithApplication {
      // And a request with the JSON body with invalid JSON.
      val request = FakeRequest().withBody(Json.parse("""{"id": 1}"""))

      // When calling the fake action with the request.
      val result = call(fakeAction, request)

      // Then the status is 400 Bad Request.
      status(result)(defaultTimeout) must_=== BAD_REQUEST
    }
  }

  "Applying unsafe white list to remove unsafe HTML tags from the JSON body" should {

    // Given a fake action that uses the whitelistingJsonUnsafe body parser.
    def fakeAction(implicit app: Application): Action[JsValue] = {
      val controller = new BodyParsers {
        override protected def controllerComponents: ControllerComponents = app.injector.instanceOf[ControllerComponents]
      }

      controller.Action(controller.whitelistingJsonUnsafe) { request =>
        Ok(request.body)
      }
    }

    "clean the JSON values" in new WithApplication {
      // And a request with a JSON body with HTML tags.
      val request = FakeRequest().withBody(Json.parse("""{"name": "<script>Hack</script><span>safe</span>"}"""))

      // When calling the fake action with the request.
      val result = call(fakeAction, request)

      // Then the response content must have the JSON body cleaned.
      contentAsString(result)(defaultTimeout) must_=== """{"name":"<span>safe</span>","nameUnsafe":"<script>Hack</script><span>safe</span>"}"""

      // And the status is 200 OK.
      status(result)(defaultTimeout) must_=== OK
    }
  }
}

final case class MockTestModel(id: Int, name: String)

object MockTestModel {
  implicit val jsonFormatter: Format[MockTestModel] = Json.format[MockTestModel]
}
