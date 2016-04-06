package au.com.agiledigital.rest.security

import play.api.libs.json.JsValue
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse

import scala.concurrent.ExecutionContext

/**
 * Security focused body parsers.
 */
object BodyParsers {
  /**
   * A JSON body parser that applies HTML whitelisting rules to JSON strings.
   */
  def whitelistingJson(implicit executionContext: ExecutionContext): BodyParser[JsValue] = parse.json map clean

  /**
   * Applies an empty HTML whitelist filter to a JsValue. If the JsValue is a JsString, all HTML elements are removed
   * from the string. If the JsValue is a JsArray or JsObject, the whitelist is applied recursively.
   * All other JsValue types are left untouched.
   * @param jsValue The JsValue to clean.
   * @return The JsValue after being cleaned using the empty HTML whitelist.
   */
  def clean(jsValue: JsValue): JsValue = HtmlWhitelistFilter.applyFilter(jsValue, HtmlWhitelistFilter.removeAll, includeUnsafe = true)
}
