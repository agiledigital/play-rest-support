package au.com.agiledigital.rest.security

import au.com.agiledigital.rest.tests.BaseSpec
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.DataTables
import play.api.libs.json._

/**
 * Tests for [[BodyParsers]].
 */
class BodyParsersSpec(implicit ev: ExecutionEnv) extends BaseSpec with DataTables {
  "BodyParsers clean" should {

    val jsString = JsString("<img src=\"foo\"><span>bar</span><script>alert('baz');</script> baz")

    val jsArray = Json.arr(Json.arr("<img src=\"foo\"><span>bar</span><script>alert('baz');</script> baz"))

    val expectedArray = Json.arr(Json.arr("bar baz"))

    val expectedUnsafeArray = Json.arr(Json.arr("<span>bar</span> baz"))

    val jsObject = Json.obj(
      "foo" -> Json.obj(
        "bar" -> "<img src=\"foo\"><span>bar</span><script>alert('baz');</script> baz"
      )
    )

    val expectedObject = Json.obj(
      "foo" -> Json.obj(
        "bar" -> "bar baz",
        "barUnsafe" -> "<img src=\"foo\"><span>bar</span><script>alert('baz');</script> baz"
      ),
      "fooUnsafe" -> Json.obj(
        "bar" -> "<img src=\"foo\"><span>bar</span><script>alert('baz');</script> baz"
      )
    )

    val expectedUnsafeObject = Json.obj(
      "foo" -> Json.obj(
        "bar" -> "<span>bar</span> baz",
        "barUnsafe" -> "<img src=\"foo\"><span>bar</span><script>alert('baz');</script> baz"
      ),
      "fooUnsafe" -> Json.obj(
        "bar" -> "<img src=\"foo\"><span>bar</span><script>alert('baz');</script> baz"
      )
    )

    // @formatter:off
    "clean JSON values using the specified HTML whitelist" ||
      "description"                       || "json"               || "expected clean all" || "expected clean unsafe"          |>
      "leave non-HTML strings alone"      !! JsString("bar baz")  !! JsString("bar baz")  !! JsString("bar baz")              |
      "remove HTML elements"              !! jsString             !! JsString("bar baz")  !! JsString("<span>bar</span> baz") |
      "apply recursively to JSON arrays"  !! jsArray              !! expectedArray        !! expectedUnsafeArray              |
      "apply recursively to JSON objects" !! jsObject             !! expectedObject       !! expectedUnsafeObject             |
      "leave JSON numbers alone"          !! JsNumber(42)         !! JsNumber(42)         !! JsNumber(42)                     |
      "leave JSON null alone"             !! JsNull               !! JsNull               !! JsNull|
      "leave JSON bools alone"            !! JsBoolean(false)     !! JsBoolean(false)     !! JsBoolean(false)                 |> {
      (description, json, expectedCleanAll, expectedUnsafe) => {
        // @formatter:on
        BodyParsers.clean(json) must beEqualTo(expectedCleanAll)

        BodyParsers.cleanUnsafe(json) must beEqualTo(expectedUnsafe)
      }
    }
  }
}
