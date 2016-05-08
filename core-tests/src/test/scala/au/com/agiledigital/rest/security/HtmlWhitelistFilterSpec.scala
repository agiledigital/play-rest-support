package au.com.agiledigital.rest.security

import au.com.agiledigital.rest.tests.BaseSpec
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.DataTables

/**
  * Contains unit tests for the [[HtmlWhitelistFilter]].
  */
class HtmlWhitelistFilterSpec(implicit ev: ExecutionEnv) extends BaseSpec with DataTables {
  "HtmlWhitelistFilter remove unsafe" should {
    // format: OFF
    "remove unsafe HTML tags from the input string" ||
      "description"                             || "input string"                      || "expected"                                                 |>
      "leave non-HTML strings alone"            !! "This is a string"                  !! "This is a string"                                         |
      "remove unsafe attribute from the string" !! "<p style=\"display: none\"></p>"   !! "<p></p>"                                                  |
      "remove unsafe tags from the string"      !! "<img>unsafe tags<scripts>"         !! "unsafe tags"                                              |
      "allow heading for the string"            !! "<h1></h1>"                         !! "<h1></h1>"                                                |
      "remove DIV tag"                          !! "<div>This is a div</div>"          !! "This is a div"                                            |
      "allow anchor attributes"                 !! "<a title=\"test\" target=\"new\">" !! "<a title=\"test\" target=\"new\" rel=\"nofollow\"></a>"   |>{
      (description, inputString, expected) => {
            // format: ON
            HtmlWhitelistFilter.removeUnsafe(inputString) must beEqualTo(expected)
          }
      }
  }

  "HtmlWhitelistFilter remove all" should {
    // format: OFF
    "remove all HTML tags from the input string" ||
      "description"                  || "input string"               || "expected"                    |>
      "leave non-HTML strings alone" !! "<p>This is a string</p>"    !! "This is a string"            |
      "remove any HTML tags"         !! "<p></p><img><b></b><a></a>" !! ""                            |>{
      (description, inputString, expected) => {
            // format: ON
            HtmlWhitelistFilter.removeAll(inputString) must beEqualTo(expected)
          }
      }
  }
}
