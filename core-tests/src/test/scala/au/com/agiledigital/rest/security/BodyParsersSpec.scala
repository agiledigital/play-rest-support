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

    // format: OFF
    "clean JSON values using the HTML whitelist" ||
      "description"                       || "json"               || "expected"          |>
      "leave non-HTML strings alone"      !! JsString("bar baz")  !! JsString("bar baz") |
      "remove HTML elements"              !! jsString             !! JsString("bar baz") |
      "apply recursively to JSON arrays"  !! jsArray              !! expectedArray       |
      "apply recursively to JSON objects" !! jsObject             !! expectedObject      |
      "leave JSON numbers alone"          !! JsNumber(42)         !! JsNumber(42)        |
      "leave JSON null alone"             !! JsNull               !! JsNull              |
      "leave JSON bools alone"            !! JsBoolean(false)     !! JsBoolean(false)    |> {
      (description, json, expected) => {
            // format: ON
            BodyParsers.clean(json) must beEqualTo(expected)
          }
      }
  }
}
