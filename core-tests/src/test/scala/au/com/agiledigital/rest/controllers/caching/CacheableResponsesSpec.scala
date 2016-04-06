package au.com.agiledigital.rest.controllers.caching

import au.com.agiledigital.rest.tests.BaseSpec
import org.joda.time.DateTimeUtils
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.DataTables
import play.api.mvc.{Action, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Contains tests for the [[CacheableResponses]] action builder.
 */
class CacheableResponsesSpec(implicit ev: ExecutionEnv) extends BaseSpec with DataTables with CacheableResponses {

  sequential

  DateTimeUtils.setCurrentMillisFixed(5000)

  "Wrapping the action should" should {
    // @formatter:off
     "produce an action that sets cache headers as expected" ||
       "description"     || "status"     || "duration"   || "wrapped headers"            || "expected"  |>
       "200, 60 seconds" !! OK           !! (60 seconds) !! emptyInputHeaders            !! resultHeadersWithCacheControl(5000 + 60000, "60")  |
       "200, 60 seconds" !! OK           !! (60 seconds) !! inputHeadersWithCacheControl !! resultHeadersWithCacheControl(5000 + 60000, "60") |
       "200, 60 seconds" !! OK           !! (60 seconds) !! inputHeadersWithExpiry       !! resultHeadersWithCacheControl(5000 + 60000, "60")  |
       "200, 60 seconds" !! OK           !! (60 seconds) !! inputHeadersWithRandom       !! inputHeadersWithRandom.toMap ++ resultHeadersWithCacheControl(5000 + 60000, "60")  |
       "404, 60 seconds" !! NOT_FOUND    !! (60 seconds) !! inputHeadersWithRandom       !! inputHeadersWithRandom |
       "200, 1 hour"     !! OK           !! (1 hour)     !! emptyInputHeaders            !! resultHeadersWithCacheControl(5000 + 60000 * 60, "3600")  |> {
        (description, resultStatus, duration, resultHeaders, expectedHeaders) => {
         // @formatter:on

          // Given an action that returns the supplied status and headers
          val action = Action.async { request =>
            Future.successful(Results.Status(resultStatus).withHeaders(resultHeaders: _*))
          }

          // That has been wrapped in the cacheable action.
          val cacheableAction = cacheable(duration)(action)

          // When the action is called.
          val actual = cacheableAction.apply(FakeRequest("get", "something"))

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

  val emptyResultHeaders = Map[String, String]()

  def resultHeadersWithCacheControl(expiry: Long, maxAge: String) = emptyResultHeaders + (EXPIRES -> expiry.toString) + (CACHE_CONTROL -> s"max-age=$maxAge")
}
