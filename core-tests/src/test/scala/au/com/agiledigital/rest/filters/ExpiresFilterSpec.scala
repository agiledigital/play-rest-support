package au.com.agiledigital.rest.filters

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import au.com.agiledigital.rest.tests.{ BaseSpec }
import com.sun.jna.platform.win32.Guid.GUID
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.DataTables
import org.specs2.specification.After
import play.api.mvc.{ RequestHeader, Result, Results }
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.{ Await, Future }
import scala.language.postfixOps

/**
  * Contains tests for the [[ExpiresFilter]].
  */
class ExpiresFilterSpec(implicit ev: ExecutionEnv) extends BaseSpec with DataTables {

  "Filtering a result" should {
    // format: OFF
    "add headers as expected" ||
      "description"                   || "status"     || "headers"                             || "expected headers"  |>
      "200, no headers"               !! OK           !! emptyInputHeaders                     !! resultHeadersWithExpiry  |
      "404, no headers"               !! NOT_FOUND    !! emptyInputHeaders                     !! emptyResultHeaders  |
      "200, random header"            !! OK           !! inputHeadersWithRandom                !! resultHeadersWithExpiry + ("some_header" -> "some_header_value") |
      "200, cache-control header"     !! OK           !! inputHeadersWithCacheControl          !! emptyResultHeaders + (CACHE_CONTROL -> "no-cache") |
      "200, last modified header"     !! OK           !! inputHeadersWithLastModified          !! emptyResultHeaders + (LAST_MODIFIED -> "some-time") |
      "200, cache-control and random" !! OK           !! inputHeadersWithCacheControlAndRandom !! emptyResultHeaders  + (CACHE_CONTROL -> "no-cache") + ("some_header" -> "some_header_value") |
      "200, expires header"           !! OK           !! inputHeadersWithExpiry                !! emptyResultHeaders + (EXPIRES -> "1000") |> {
       (outerDescription, outerResultStatus, outerResultHeaders, outerExpectedHeaders) => new WithActorSystem {
            // format: ON
            val description = outerDescription
            val resultStatus = outerResultStatus
            val resultHeaders = outerResultHeaders
            val expectedHeaders = outerExpectedHeaders

            // Given a handler that returns the supplied status and headers
            val handler: (RequestHeader) => Future[Result] = _ => Future.successful(
              Results.Status(resultStatus).withHeaders(resultHeaders: _*)
            )

            // When the filter is called.
            val filter = new ExpiresFilter()(ActorMaterializer.create(system), ev.executionContext)
            val actual = filter(handler)(FakeRequest("get", "something"))

            // Then the status should have been returned unchanged.
            status(actual) must_== resultStatus

            // And the headers should match the expected ones.
            headers(actual) must_== expectedHeaders
          }
      }
  }

  val emptyInputHeaders = Seq[(String, String)]()

  val inputHeadersWithRandom = emptyInputHeaders :+ "some_header" -> "some_header_value"

  val inputHeadersWithExpiry = emptyInputHeaders :+ EXPIRES -> "1000"

  val inputHeadersWithCacheControl = emptyInputHeaders :+ CACHE_CONTROL -> "no-cache"

  val inputHeadersWithCacheControlAndRandom = inputHeadersWithCacheControl ++ inputHeadersWithRandom

  val inputHeadersWithLastModified = emptyInputHeaders :+ LAST_MODIFIED -> "some-time"

  val emptyResultHeaders = Map[String, String]()

  val resultHeadersWithExpiry = emptyResultHeaders + (EXPIRES -> "-1")

  trait WithActorSystem extends After {

    lazy val system = ActorSystem("test-" + UUID.randomUUID().toString)

    def after: Any = Await.result(system.terminate(), defaultAwait)
  }

}
