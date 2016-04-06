package au.com.agiledigital.rest.security

import javax.inject._

import au.com.agiledigital.rest.controllers.transport.JsonApiResponse
import play.api._
import play.api.http.Status._
import play.api.http.{DefaultHttpErrorHandler, MimeTypes}
import play.api.mvc._
import play.api.routing.Router

import scala.concurrent._

/**
 * Provides JSON error responses when the requestor accepts JSON. If the requestor does not accept a JSON response,
 * defers handling back to the [[DefaultHttpErrorHandler]].
 *
 * @param env the environment of the application (chiefly, whether it is in DEV or PROD mode).
 * @param config the application's configuration.
 * @param sourceMapper the source mapper.
 * @param router the router provider.
 */
class ErrorHandler @Inject()(env: Environment,
                             config: Configuration,
                             sourceMapper: OptionalSourceMapper,
                             router: Provider[Router]) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    if (isJsonRequest(request)) {
      statusCode match {
        case BAD_REQUEST => JsonApiResponse.badRequestApiResponse(message, Nil)
        case FORBIDDEN => Future.successful(JsonApiResponse.forbiddenResponse(message))
        case NOT_FOUND => Future.successful(JsonApiResponse.notFoundResponse(message))
        case clientError if statusCode >= 400 && statusCode < 500 => JsonApiResponse.badRequestApiResponse(message, Nil)
        case _ => super.onClientError(request, statusCode, message)
      }
    }
    else {
      super.onClientError(request, statusCode, message)
    }
  }

  override protected def onDevServerError(request: RequestHeader, exception: UsefulException): Future[Result] = {
    if (isJsonRequest(request)) {
      Future.successful(
        JsonApiResponse.internalServerErrorResponse(s"A server error occurred - [$exception].", exception)
      )
    }
    else {
      super.onDevServerError(request, exception)
    }
  }

  override protected def onProdServerError(request: RequestHeader, exception: UsefulException) = {
    if (isJsonRequest(request)) {
      Future.successful(
        JsonApiResponse.internalServerErrorResponse(s"A server error occurred [${exception.id}].")
      )
    }
    else {
      super.onProdServerError(request, exception)
    }
  }

  private def isJsonRequest(request: RequestHeader): Boolean = request.accepts(MimeTypes.JSON)

}
