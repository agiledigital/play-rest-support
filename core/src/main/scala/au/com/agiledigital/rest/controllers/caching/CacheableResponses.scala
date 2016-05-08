package au.com.agiledigital.rest.controllers.caching

import org.joda.time.DateTimeUtils
import play.api.http.HeaderNames._
import play.api.http.Status._
import play.api.mvc.Action

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
  * Provides an action builder that marks a response as cacheable.
  */
trait CacheableResponses {

  /**
    * Builds a wrapper action around the supplied action that sets the cache headers to effect the requested degree
    * of caching. Only adds the cache headers if an OK status is set by the wrapped action.
    *
    * NOTE: will override the expiry and cache-control headers set by the underlying action.
    *
    * @param duration the amount of time that an OK response from the wrapped action should be cacheable for.
    * @param action the action to wrap.
    * @param ec the execution context used to map the response.
    * @tparam A type of request processed by the wrapped action.
    * @return a wrapper around the supplied response that will set the cache headers.
    */
  def cacheable[A](duration: FiniteDuration)(action: Action[A])(implicit ec: ExecutionContext): Action[A] =
    Action.async(action.parser) { request =>
      action(request) map { response =>
        if (statusCodesToSet.contains(response.header.status)) {
          response.withHeaders(
            EXPIRES -> (DateTimeUtils.currentTimeMillis() + duration.toMillis).toString,
            CACHE_CONTROL -> s"max-age=${duration.toSeconds.toString}"
          )
        }
        else {
          response
        }
      }
    }

  /** The result status codes that must be returned from the underlying action for the filter to be applied. */
  private val statusCodesToSet = Set(OK)

}
