package au.com.agiledigital.rest.filters

import javax.inject.Inject

import akka.stream.Materializer
import play.api.http.HeaderNames._
import play.api.http.Status._
import play.api.mvc.{ Filter, RequestHeader, Result }

import scala.concurrent.{ ExecutionContext, Future }

/**
  * Filters responses by adding an expires -> "-1" header if none of:
  * # last-modified
  * # cache-control
  * # expires
  * have been set and the status is 200.
  *
  * @param ec the execution context that is used to map the response from the underlying action.
  */
class ExpiresFilter @Inject() (implicit val mat: Materializer, val ec: ExecutionContext) extends Filter {
  override def apply(nextFilter: (RequestHeader) => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    nextFilter(requestHeader) map { result =>
      if (statusCodesToSet.contains(result.header.status)) {
        if (result.header.headers.keys.exists(headersToCheck.contains)) {
          result
        }
        else {
          result.withHeaders(EXPIRES -> "-1")
        }
      }
      else {
        result
      }
    }
  }

  /** The headers that, if set, indicate that an underlying action has already set cache related headers. */
  private val headersToCheck = Set(LAST_MODIFIED, CACHE_CONTROL, EXPIRES)

  /** The result status codes that must be returned from the underlying action for the filter to be applied. */
  private val statusCodesToSet = Set(OK)
}
