package au.com.agiledigital.rest.security

import javax.inject._

import au.com.agiledigital.rest.controllers.transport.{ MessageLevel, Message, JsonApiResponse }
import play.api._
import play.api.http.{ Writeable, DefaultHttpErrorHandler, MimeTypes }
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc._
import play.api.routing.Router

import scala.concurrent.{ Future, ExecutionContext }

/**
  * Provides JSON error responses when the requestor accepts JSON. If the requestor does not accept a JSON response,
  * defers handling back to the [[DefaultHttpErrorHandler]].
  *
  * @param env the environment of the application (chiefly, whether it is in DEV or PROD mode).
  * @param config the application's configuration.
  * @param sourceMapper the source mapper.
  * @param router the router provider.
  */
class ErrorHandler @Inject() (
    env: Environment,
    config: Configuration,
    sourceMapper: OptionalSourceMapper,
    router: Provider[Router]
)(implicit val ec: ExecutionContext) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    if (isJsonRequest(request)) {
      super.onClientError(request, statusCode, message).map { result =>
        val writeable: Writeable[JsValue] = implicitly[Writeable[JsValue]]
        Result(result.header, writeable.toEntity(Json.toJson(JsonApiResponse[String](None, Message(message, MessageLevel.Error)))))
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
