package au.com.agiledigital.rest.security

import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue}

/**
 * Exposes HTML whitelist filtering operations.
 */
object HtmlWhitelistFilter {
  /**
   * Applies an empty HTML whitelist filter to the specified value, removing all HTML elements.
   * @param value The string from which to remove HTML elements.
   * @return The cleaned string after having HTML elements removed.
   */
  def removeAll(value: String): String = Jsoup.clean(value, Whitelist.none())

  /**
   * Applies a basic custom HTML whitelist filter to the specified value, removing unsafe elements
   * such as script, style and img tags.
   * @param value The string from which to remove unsafe HTML elements.
   * @return The cleaned string after having unsafe HTML elements removed.
   */
  def removeUnsafe(value: String): String = Jsoup.clean(value,
    Whitelist.basic()
      .addTags("h1", "h2", "h3", "h4", "h5", "h6")
      .addAttributes("a", "href", "title", "target"))

  /**
   * Applies a filter to a JsValue. If the JsValue is a JsString, the filter is applied to the string.
   * If the JsValue is a JsArray or JsObject, the filter is applied recursively.
   * All other JsValue types are left untouched.
   * @param jsValue The JsValue to filter.
   * @param filter The filter to apply.
   * @param includeUnsafe True if the original, unfiltered values should be included in new fields
   *                      suffixed by "Unsafe", false otherwise.
   * @return The JsValue after being filtered.
   */
  def applyFilter(jsValue: JsValue, filter: String => String, includeUnsafe: Boolean): JsValue = {

    def applyFilterRecursive(jsValue: JsValue) = applyFilter(jsValue, filter, includeUnsafe)

    jsValue match {
      case JsString(value) => JsString(filter(value))
      case JsArray(value) => JsArray(value map applyFilterRecursive)
      case JsObject(values) =>
        val cleanedValues = values mapValues applyFilterRecursive
        val renamedValues = if (includeUnsafe) for ((k, v) <- values) yield k + "Unsafe" -> v else Nil
        JsObject(cleanedValues ++ renamedValues)
      case v => v
    }
  }
}
