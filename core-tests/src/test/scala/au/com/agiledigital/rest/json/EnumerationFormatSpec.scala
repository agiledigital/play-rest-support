package au.com.agiledigital.rest.json

import au.com.agiledigital.rest.tests.BaseSpec
import org.specs2.matcher.DataTables
import play.api.libs.json.{ JsNumber, JsError, JsString, JsSuccess }

/**
  * Contains unit tests for the [[EnumerationFormat]].
  */
class EnumerationFormatSpec extends BaseSpec with DataTables {

  "Writing an enumeration to JSON" should {
    // format: OFF
     "use the name of the enumeration value" ||
       "description"     || "value"               || "expected"        |>
       "info"            !! TestEnumeration.Info  !! JsString("Info")  |
       "ERROR"           !! TestEnumeration.Error !! JsString("ERROR") |> {
        (description, enumerationValue, expectedJson) => {
            // format: ON

            // Given an enumeration value

            // When it is written to JSON
            val actual = EnumerationFormat.format(TestEnumeration).writes(enumerationValue)

            // Then the expected JSON should have been written.
            actual must_=== expectedJson
          }
      }
  }

  "Reading an enumeration from JSON" should {
    // format: OFF
     "use the name of the enumeration value" ||
       "description"     || "value"           || "expected"   |>
       "not a JsString"  !! JsNumber(99)      !! JsError("Input for class au.com.agiledigital.rest.json.TestEnumeration$ should be a JsString, got a [class play.api.libs.json.JsNumber] - [99].") |
       "unknown value"   !! JsString("warn")  !! JsError("Unknown class au.com.agiledigital.rest.json.TestEnumeration$ [warn], accepted values are [Info,ERROR].") |
       "info"            !! JsString("Info")  !! JsSuccess(TestEnumeration.Info)   |
       "ERROR"           !! JsString("ERROR") !! JsSuccess(TestEnumeration.Error)  |> {
        (description, json, expectedJsResult) => {
            // format: ON

            // Given some input JSON

            // When it is read to the enumeration
            val actual = EnumerationFormat.format(TestEnumeration).reads(json)

            // Then the expected JsResult should be returned.
            actual must_=== expectedJsResult
          }
      }
  }
}

object TestEnumeration extends Enumeration {
  def TestEnumeration = Value

  val Info = Value(0, "Info")
  val Error = Value(1, "ERROR")
}
