package au.com.agiledigital.rest.security

import javax.inject.Provider

import au.com.agiledigital.rest.tests.{ BaseSpec, NoStackTraceException }
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.{ DataTables, JsonMatchers }
import play.api._
import play.api.http.{ HeaderNames, MimeTypes }
import play.api.routing.Router
import play.api.test.FakeRequest
import play.api.test.Helpers._

/**
  * Contains units tests for the [[ErrorHandler]].
  */
class ErrorHandlerSpec(implicit ev: ExecutionEnv) extends BaseSpec with DataTables with JsonMatchers {

  "Handling a client error" should {
    "produce a JSON response when appropriate" ||
      // format: OFF
      "description"                        || "request"            || "status code" || "expected response" |>
      "accepts all types"                  !! acceptsAll           !! 400           !! 400 |
      "empty accepts"                      !! emptyAccepts         !! 403           !! 403 |
      "accepts json"                       !! acceptsJson          !! 408           !! 408 |
      "accepts multiple (including json)"  !! acceptsJsonAndOthers !! 404           !! 404 |>  {
      (description, request, statusCode, expected) => {
            // format: ON

            // Given an error handler that is in DEV mode.
            val errorHandler = new ErrorHandler(
              Environment.simple(mode = Mode.Dev),
              Configuration.empty,
              new OptionalSourceMapper(None),
              new Provider[Router] {
                override def get(): Router = Router.empty
              }
            )

            // When it is used to handle a client error.
            val result = errorHandler.onClientError(request, statusCode, "the message")

            // Then the expected status code should have been returned.
            status(result) must_=== expected

            // And should contain a JSON response.
            val json = contentAsString(result)

            // That contains the expected message.
            json must /("messages") /# 0 / ("message" -> "the message")
            json must /("messages") /# 0 / ("level" -> "Error")
          }
      }
    "delegate to the default handler when the requestor does not accept JSON" ||
      // format: OFF
      "description"                 || "request"            |>
      "accepts text"                !! acceptsText          |
      "accepts javascript"          !! acceptsJavascript    |>  {
      (description, request) => {
            // format: ON

            // Given an error handler that is in DEV mode.
            val errorHandler = new ErrorHandler(
              Environment.simple(mode = Mode.Dev),
              Configuration.empty,
              new OptionalSourceMapper(None),
              new Provider[Router] {
                override def get(): Router = Router.empty
              }
            )

            // When it is used to handle a client error.
            val result = errorHandler.onClientError(request, 400, "the message")

            // Then the expected status code should be returned.
            status(result) must_=== 400

            // But it should not have been JSON.
            contentType(result) must not beSome MimeTypes.JSON
          }
      }
  }

  "Handling an error in development mode" should {
    "produce a JSON response when appropriate" ||
      // format: OFF
      "description"                        || "request"            || "expected response" |>
      "accepts all types"                  !! acceptsAll           !! "" |
      "empty accepts"                      !! emptyAccepts         !! "" |
      "accepts json"                       !! acceptsJson          !! "" |
      "accepts multiple (including json)"  !! acceptsJsonAndOthers !! "" |>  {
      (description, request, expected) => {
            // format: ON

            // Given an error handler that is in DEV mode.
            val errorHandler = new ErrorHandler(
              Environment.simple(mode = Mode.Dev),
              Configuration.empty,
              new OptionalSourceMapper(None),
              new Provider[Router] {
                override def get(): Router = Router.empty
              }
            )

            // When it is used to handle an error
            val handledException = new NoStackTraceException(description)
            val result = errorHandler.onServerError(request, handledException)

            // Then a 500 should always be returned.
            status(result) must_=== INTERNAL_SERVER_ERROR

            // And should contain a JSON response.
            val json = contentAsString(result)

            // That contains the expected message.
            json must /("messages") /# 0 / ("message" -> matching("A server error occurred - \\[.*: Execution exception in null:null\\]\\."))
            json must /("messages") /# 0 / ("level" -> "Error")
            // And the expected exception.
            json must /("result") / "cause" / ("message" -> handledException.getMessage)
          }
      }
    "delegate to the default handler when the requestor does not accept JSON" ||
      // format: OFF
      "description"                 || "request"            || "expected response"    |>
      "accepts text"                !! acceptsText          !! "" |
      "accepts javascript"          !! acceptsJavascript    !! "" |>  {
      (description, request, expected) => {
            // format: ON

            // Given an error handler that is in DEV mode.
            val errorHandler = new ErrorHandler(
              Environment.simple(mode = Mode.Dev),
              Configuration.empty,
              new OptionalSourceMapper(None),
              new Provider[Router] {
                override def get(): Router = Router.empty
              }
            )

            // When it is used to handle an error
            val result = errorHandler.onServerError(request, new NoStackTraceException(description))

            // Then a 500 should always be returned.
            status(result) must_=== INTERNAL_SERVER_ERROR

            // But it should not have been JSON.
            contentType(result) must not beSome MimeTypes.JSON
          }
      }
  }

  "Handling an error in production mode" should {
    "produce a JSON response when appropriate" ||
      // format: OFF
      "description"                        || "request"            || "expected response" |>
      "accepts all types"                  !! acceptsAll           !! "" |
      "empty accepts"                      !! emptyAccepts         !! "" |
      "accepts json"                       !! acceptsJson          !! "" |
      "accepts multiple (including json)"  !! acceptsJsonAndOthers !! "" |>  {
      (description, request, expected) => {
            // format: ON

            // Given an error handler that is in PROD mode.
            val errorHandler = new ErrorHandler(
              Environment.simple(mode = Mode.Prod),
              Configuration.empty,
              new OptionalSourceMapper(None),
              new Provider[Router] {
                override def get(): Router = Router.empty
              }
            )

            // When it is used to handle an error
            val handledException = new NoStackTraceException(description)
            val result = errorHandler.onServerError(request, handledException)

            // Then a 500 should always be returned.
            status(result) must_=== INTERNAL_SERVER_ERROR

            // And should contain a JSON response.
            val json = contentAsString(result)

            // That contains the expected message.
            json must /("messages") /# 0 / ("message" -> matching("A server error occurred \\[.*\\]\\."))
            json must /("messages") /# 0 / ("level" -> "Error")
            // But NO exception.
            json must not / "result"
          }
      }
    "delegate to the default handler when the requestor does not accept JSON" ||
      // format: OFF
      "description"                 || "request"            || "expected response"    |>
      "accepts text"                !! acceptsText          !! "" |
      "accepts javascript"          !! acceptsJavascript    !! "" |>  {
      (description, request, expected) => {
            // format: ON

            // Given an error handler that is in PROD mode.
            val errorHandler = new ErrorHandler(
              Environment.simple(mode = Mode.Prod),
              Configuration.empty,
              new OptionalSourceMapper(None),
              new Provider[Router] {
                override def get(): Router = Router.empty
              }
            )

            // When it is used to handle an error
            val result = errorHandler.onServerError(request, new NoStackTraceException(description))

            // Then a 500 should always be returned.
            status(result) must_=== INTERNAL_SERVER_ERROR

            // But it should not have been JSON.
            contentType(result) must not beSome MimeTypes.JSON
          }
      }
  }

  val acceptsAll = FakeRequest().withHeaders(HeaderNames.ACCEPT -> "*/*")

  val emptyAccepts = FakeRequest()

  val acceptsText = FakeRequest().withHeaders(HeaderNames.ACCEPT -> MimeTypes.TEXT)

  val acceptsJson = FakeRequest().withHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON)

  val acceptsJavascript = FakeRequest().withHeaders(HeaderNames.ACCEPT -> MimeTypes.JAVASCRIPT)

  val acceptsJsonAndOthers = FakeRequest().withHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON, HeaderNames.ACCEPT -> MimeTypes.TEXT)

}
