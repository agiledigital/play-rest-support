package au.com.agiledigital.rest.json

import java.util.NoSuchElementException

import play.api.libs.json._

import scala.util.Try

/**
 * Creates JSON Formats for Enumerations.
 */
object EnumerationFormat {
  /**
   * Creates JSON Formats for scala Enumerations.
   * @return The JSON Format for the given Enumerations.
   */
  def format[A <: Enumeration](e: A): Format[A#Value] = {
    new Format[A#Value] {

      val className = e.getClass

      override def writes(o: A#Value): JsValue = JsString(o.toString)

      override def reads(json: JsValue): JsResult[A#Value] = json match {
        case JsString(name) =>
          Try {
            JsSuccess(e.withName(name))
          } recover {
            case nse: NoSuchElementException =>
              JsError(s"""Unknown $className [$name], accepted values are [${e.values.mkString(",")}].""")
          } getOrElse {
            JsError(s"""Unknown $className [$name], accepted values are [${e.values.mkString(",")}].""")
          }
        case u => JsError(s"Input for $className should be a JsString, got a [${u.getClass}] - [$u].")
      }
    }
  }
}

