package au.com.agiledigital.rest.security

import au.com.agiledigital.rest.controllers.transport.JsonApiResponse
import play.api.libs.json.{ JsValue, Reads }
import play.api.mvc.{ BodyParser, Result }
import play.api.mvc.BodyParsers.parse

import scala.concurrent.ExecutionContext

/**
  * Security focused body parsers.
  */
object BodyParsers {
  /**
    * A JSON body parser that applies HTML whitelisting rules to JSON strings and validate based on the type, this removes all HTML tags.
    */
  def whitelistingJson[A](implicit executionContext: ExecutionContext, reader: Reads[A]): BodyParser[A] = parse.json map clean validate as[A]

  /**
    * A JSON body parser that applies HTML whitelisting rules to JSON strings, this removes all HTML tags.
    */
  def whitelistingJson(implicit executionContext: ExecutionContext): BodyParser[JsValue] = parse.json map clean

  /**
    * A JSON body parser that applies HTML whitelisting rules to JSON strings that allows a set of unsafe HTML characters to pass through.
    * It returns the cleaned and validated value.
    * It allows the HTML tags in basic set of [[org.jsoup.safety.Whitelist]].
    */
  def whitelistingJsonUnsafe[A](implicit executionContext: ExecutionContext, reader: Reads[A]): BodyParser[A] = parse.json map cleanUnsafe validate as[A]

  /**
    * A JSON body parser that applies HTML whitelisting rules to JSON strings that allows a set of unsafe HTML characters to pass through.
    * It allows the HTML tags in basic set of [[org.jsoup.safety.Whitelist]].
    */
  def whitelistingJsonUnsafe(implicit executionContext: ExecutionContext): BodyParser[JsValue] = parse.json map cleanUnsafe

  /**
    * Applies an unsafe HTML whitelist filter to a JsValue. If the JsValue is a JsString, all HTML elements are removed
    * from the string. If the JsValue is a JsArray or JsObject, the whitelist is applied recursively. Also it will not include unsafe.
    * All other JsValue types are left untouched.
    * @param jsValue The JsValue to clean.
    * @return The JsValue after being cleaned using the empty HTML whitelist.
    */
  def cleanUnsafe(jsValue: JsValue): JsValue = HtmlWhitelistFilter.applyFilter(jsValue, HtmlWhitelistFilter.removeUnsafe, includeUnsafe = true)

  /**
    * Applies an empty HTML whitelist filter to a JsValue. If the JsValue is a JsString, all HTML elements are removed
    * from the string. If the JsValue is a JsArray or JsObject, the whitelist is applied recursively.
    * All other JsValue types are left untouched.
    * @param jsValue The JsValue to clean.
    * @return The JsValue after being cleaned using the empty HTML whitelist.
    */
  def clean(jsValue: JsValue): JsValue = HtmlWhitelistFilter.applyFilter(jsValue, HtmlWhitelistFilter.removeAll, includeUnsafe = true)

  /**
    * Validates a specified Json based on given type.
    * @param jsValue The JsValue to validate.
    * @param reads The JSON reader to read the given Json for type [[A]].
    * @return Either validated type or bad request.
    */
  private def as[A](jsValue: JsValue)(implicit reads: Reads[A]): Either[Result, A] =
    jsValue.validate[A].asEither.left.map(JsonApiResponse.badRequestResponse("JSON validation error", _))

}
