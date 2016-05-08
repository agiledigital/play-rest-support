package au.com.agiledigital.rest.controllers

import play.api.Logger
import play.api.mvc.{ Action, BodyParser, Request, Result }

import scala.concurrent.Future

/**
  * Contains common Actions (and future ActionFunctions).
  */
object Actions {

  /**
    * Wraps an action to log the fact that a temporary action has been called.
    * @param action the wrapped action.
    * @tparam A the type of the body of the wrapped action.
    */
  final case class TemporaryAction[A](action: Action[A]) extends Action[A] {
    override def parser: BodyParser[A] = action.parser

    override def apply(request: Request[A]): Future[Result] = {
      Logger.error(s"Calling temporary action [${request.path}].")
      action(request)
    }
  }

}
