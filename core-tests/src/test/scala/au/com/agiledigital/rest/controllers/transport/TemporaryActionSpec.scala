package au.com.agiledigital.rest.controllers.transport

import au.com.agiledigital.rest.controllers.Actions.TemporaryAction
import au.com.agiledigital.rest.tests.BaseSpec
import org.specs2.concurrent.ExecutionEnv
import play.api.libs.json.{ JsString, JsValue }
import play.api.mvc.Results._
import play.api.mvc.{ Action, _ }
import play.api.test.{ FakeRequest, WithApplication }

/**
  * Contains unit tests for the [[au.com.agiledigital.rest.controllers.Actions.TemporaryAction]].
  */
class TemporaryActionSpec(implicit ev: ExecutionEnv) extends BaseSpec {

  "Wrapping an action with the temporary action" should {
    "not affect the result of calling the underlying action" in new WithApplication with play.api.test.Injecting {

      val Action = inject[DefaultActionBuilder]
      val parse = inject[PlayBodyParsers]

      val originalRequest = FakeRequest().withBody(JsString("input"))

      val response = Ok("original result")

      // Given an Action
      val action: Action[JsValue] = Action(parse.json) { request =>
        // That checks the request is passed through unchanged.
        request must_== originalRequest

        // And returns a response.
        response
      }

      // That has been wrapped in a temporary action.
      val wrappedAction = TemporaryAction(action)

      // When it is invoked.
      val result = wrappedAction(originalRequest)

      // Then the response should have been returned.
      result must beEqualTo(response).awaitFor(defaultAwait)
    }
  }
}
