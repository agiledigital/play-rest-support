package au.com.agiledigital.rest.controllers.transport

import akka.actor.ActorSystem
import au.com.agiledigital.rest.controllers.transport.MessageLevel.MessageLevel
import au.com.agiledigital.rest.json.EnumerationFormat
import au.com.agiledigital.rest.json.ThrowableWrites._
import au.com.agiledigital.rest.security.HtmlWhitelistFilter
import play.api.Logger
import play.api.data.FormError
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scalaz.{-\/, \/, \/-}

/**
 * Encapsulates a response T and any messages that were produced while
 * processing the request that produced T.
 */
final case class JsonApiResponse[T](result: Option[T], messages: Message*)

/**
 * The severity (level) of messages that can be produced whilst processing an API request.
 */
object MessageLevel extends Enumeration {
  type MessageLevel = Value
  // Info messages are intended to be used to provide confirmation that the
  // requested action was carried out successfully. Users to the API should
  // generally have enough context to know what the successful processing of
  // request should mean.
  val Info = Value(0, "Info")

  // Error messages are intended to be used when a business or validation rule
  // has been violated. For example, if a price field contains a string of
  // chars an error would be returned or if a store with no menu is opened.
  val Error = Value(1, "Error")

  // Alert messages are intended to be used when prompt action is
  // required by the user. For example, if a password must be changed.
  val Alert = Value(2, "Alert")
}

/**
 * Companion object for ApiResponse that contains JSON serialisation.
 */
object JsonApiResponse {
  implicit def responseContainerWrites[T](implicit fmt: Writes[T]): Writes[JsonApiResponse[T]] = new Writes[JsonApiResponse[T]] {

    /**
     * Converts a JsonApiResponse to JSON. All strings in the JSON are passed through a filter to remove unsafe HTML.
     */
    def writes(responseContainer: JsonApiResponse[T]) = {
      val response = Json.obj("result" -> responseContainer.result, "messages" -> responseContainer.messages)

      HtmlWhitelistFilter.applyFilter(response, HtmlWhitelistFilter.removeUnsafe, includeUnsafe = false)
    }
  }

  implicit def responseContainerReads[T](implicit reads: Reads[T]): Reads[JsonApiResponse[T]] = (
    (JsPath \ "result").readNullable[T](reads) and
    (JsPath \ "messages").read[Seq[Message]]
  )((result, messages) => JsonApiResponse(result, messages:_*))

  /**
   * Builds a response with the supplied message and result.
   * @param message the message that describes the result.
   * @param result the result.
   * @param tjs the JSON writer for the result.
   * @tparam A the type of the Result.
   * @return a HTTP result.
   */
  def buildResponse[A](message: String, result: A)(implicit tjs: Writes[A]): Result =
    buildResponseForErrorsOrSuccess(message, Right(result))

  /**
   * Builds the response for the supplied Either.
   *
   * If a Left(errorMessage) is supplied then a BadRequest with the error message (as JSON response) is built.
   *
   * If a Right(successObject) is returned then an Ok with the supplied
   * success message and success object (as a JSON response) is built.
   *
   * An implicit (or explicit) JSON Writes must be available for the success object.
   */
  def buildResponseForErrorOrSuccess[A](successMessage: String, errorOrResponse: Either[String, A])(implicit tjs: Writes[A]): Result = {
    errorOrResponse match {
      case Left(errorMessage) => buildResponseForErrorsOrSuccess[String](successMessage, Left(Seq(errorMessage)))
      case Right(successObject) => buildResponseForErrorsOrSuccess(successMessage, Right(successObject))
    }
  }

  /**
   * Builds the response for the supplied Scalaz \/ (disjunction).
   *
   * If a -\/(errorMessage) is supplied then a BadRequest with the error message (as JSON response) is built.
   *
   * If a \/-(successObject) is returned then an Ok with the supplied
   * success message and success object (as a JSON response) is built.
   *
   * An implicit (or explicit) JSON Writes must be available for the success object.
   */
  def buildResponseForErrorOrSuccess[A](successMessage: String, errorOrResponse: \/[String, A])(implicit tjs: Writes[A]): Result = {
    errorOrResponse match {
      case -\/(errorMessage) => buildResponseForErrorsOrSuccess[String](successMessage, Left(Seq(errorMessage)))
      case \/-(successObject) => buildResponseForErrorsOrSuccess(successMessage, Right(successObject))
    }
  }

  /**
   * Builds the response for the supplied Either.
   *
   * If a Left(Seq(errorMessage)) is supplied then a BadRequest with the error messages (as JSON response) is built.
   *
   * If a Right(successObject) is returned then an Ok with the supplied
   * success message and success object (as a JSON response) is built.
   *
   * An implicit (or explicit) JSON Writes must be available for the success object.
   */
  def buildResponseForErrorsOrSuccess[A](successMessage: String, errorOrResponse: Either[Iterable[String], A])(implicit tjs: Writes[A]): Result = {
    errorOrResponse match {
      case Left(errorMessages) =>
        val errors = errorMessages.map(errorMessage => Message(errorMessage, MessageLevel.Error))
        BadRequest(Json.toJson(JsonApiResponse[String](None, errors.toSeq: _*)))
      case Right(successObject) => Ok(Json.toJson(JsonApiResponse(Some(successObject), Message(successMessage))))
    }
  }

  /**
   * Builds the response for the supplied Option.
   *
   * If a None is supplied then a NotFound with the none message (as JSON response) is built.
   *
   * If a Some(successObject) is returned then an Ok with the supplied
   * some message and success object (as a JSON response) is built.
   *
   * An implicit (or explicit) JSON Writes must be available for the success object.
   */
  def buildResponseForOption[A](someMessage: String, noneMessage: String, maybeResult: Option[A])(implicit tjs: Writes[A]): Result = {
    maybeResult match {
      case None => notFoundResponse(noneMessage)
      case Some(_) => Ok(Json.toJson(JsonApiResponse(maybeResult, Message(someMessage))))
    }
  }

  /**
   * Builds a 404 Not Found API response.
   * @param message The error message to use.
   * @return The Not Found API response.
   */
  def notFoundResponse(message: String): Result = {
    NotFound(Json.toJson(JsonApiResponse[String](None, Message(message, MessageLevel.Error))))
  }

  /**
   * Builds an Unauthorized API response.
   * @param message The error message to use.
   * @return The Unauthorized API response.
   */
  def unauthorizedResponse(message: String): Result = {
    Unauthorized(Json.toJson(JsonApiResponse[String](None, Message(message, MessageLevel.Error))))
  }

  /**
   * Builds an Forbidden API response.
   * @param message The error message to use.
   * @return The Forbidden API response.
   */
  def forbiddenResponse(message: String): Result = {
    Forbidden(Json.toJson(JsonApiResponse[String](None, Message(message, MessageLevel.Error))))
  }

  /**
   * Builds a 400 Bad Request API response.
   * @param message The error message to use.
   * @param errors The validation errors to include in the response.
   * @return The Bad Request API response.
   */
  def badRequestResponse(message: String, errors: Seq[(JsPath, Seq[ValidationError])]): Result = {
    BadRequest(Json.toJson(JsonApiResponse[String](None, Message(message, MessageLevel.Error, JsError.toJson(errors)))))
  }

  /**
   * Builds a 400 Bad Request API response.
   * @param message The error message to use.
   * @param errors The form errors to include in the response.
   * @return The Bad Request API response.
   */
  def badRequestResponseForForm(message: String, errors: Seq[FormError]): Result = {
    badRequestResponse(message, errors.map(error => JsPath() -> Seq(ValidationError(error.messages))))
  }

  /**
   * Wraps a bad response in a successful future, for immediate exit if there was an error
   * @param message The message to expose to the user
   * @param errors Any errors that were found in the JSON
   * @return A 400 'Bad Result' Successful Future
   */
  def badRequestApiResponse(message: String, errors: Seq[(JsPath, Seq[ValidationError])]): Future[Result] = {
    Future.successful(JsonApiResponse.badRequestResponse(message, errors))
  }

  /**
   * Builds a 500 Internal Server Error API response.
   * @param message The error message to use.
   * @return The Internal Server Error API response.
   */
  def internalServerErrorResponse(message: String): Result = {
    InternalServerError(Json.toJson(JsonApiResponse[String](None, Message(message, MessageLevel.Error))))
  }

  /**
   * Builds a 500 Internal Server Error API response that contains the details of the supplied exception.
   *
   * NOTE: should only be used in DEV mode as it may leak sensitive information.
   *
   * @param message The error message to use.
   * @param exception The cause of the internal server error.
   * @return The Internal Server Error API response.
   */
  def internalServerErrorResponse(message: String, exception: Exception): Result = {
    InternalServerError(Json.toJson(JsonApiResponse[Throwable](Some(exception), Message(message, MessageLevel.Error))))
  }

  /**
   * Combines the supplied future with another future that will time out after the specified duration then
   * wait for the first future to complete. The second future will return an InternalServerError Result
   * if it completes.
   *
   * @param future the future to monitor.
   * @param message the message to return to the user if the timeout is reached before the future complete.
   * @param duration the amount of time to wait for the future to complete.
   * @return a future that will either complete with the result of the supplied future or an InternalServerError.
   */
  def timeoutRequest(future: Future[Result], message: String, duration: FiniteDuration)
                    (implicit executionContext: ExecutionContext, system: ActorSystem): Future[Result] = {
    object Timeout
    val timeoutFuture = akka.pattern.after(duration, system.scheduler)(Future.successful(Timeout))
    Future.firstCompletedOf(Seq(future, timeoutFuture)).map {
      case Timeout =>
        Logger.warn(s"Timed out after [$duration] - [$message].")
        InternalServerError("Timed out")
      case result: Result => result
      case unknown => InternalServerError("Unexpected result type.")
    }
  }
}

/**
 * A message that was produced while processing a request.
 */
final case class Message(message: String, level: MessageLevel = MessageLevel.Info, context: JsObject = JsObject(Seq()))

/**
 * Companion object for Message that contains JSON serialisation of the message and its severity.
 */
object Message {

  implicit val levelFormat: Format[MessageLevel] = EnumerationFormat.format(MessageLevel)

  implicit val format: Format[Message] = Json.format[Message]
}
