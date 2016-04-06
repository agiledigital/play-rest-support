package au.com.agiledigital.rest.json

import play.api.libs.json._

/**
 * JSON writes for a Throwable.
 */
object ThrowableWrites {

  implicit val throwableWrites: Writes[Throwable] = new Writes[Throwable] {
    def writes(t: Throwable): JsValue = {
      val maybeCause = Option(t.getCause)
      Json.obj(
        "message" -> t.getMessage,
        "stacktrace" -> t.getStackTrace.map(_.toString)
      ) ++ (
        if (maybeCause.isDefined && !maybeCause.contains(t)) {
          Json.obj("cause" -> Json.toJson(t.getCause))
        }
        else {
          Json.obj()
        }
      )
    }
  }

}
