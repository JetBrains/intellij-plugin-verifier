package org.jetbrains.plugins.verifier.service.server.servlets.info

import com.jetbrains.pluginverifier.misc.HtmlBuilder
import com.jetbrains.pluginverifier.parameters.filtering.IgnoreCondition
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Web page for managing ignored problems.
 */
class IgnoredProblemsPage(private val ignoreConditions: List<IgnoreCondition>) {
  companion object {
    private const val CSS_STYLES = """
<style>
  html {
      box-sizing: border-box;
  }

  .ignored_problems_form {
      width: 700px;
      height: 500px;
      overflow: scroll;
      font-size: 16px;
      font-family: monospace;
  }

  .label {
      min-width: 150px;
  }

  .field {
      width: 150px;
  }

  .labeled_field {
      display: flex;
  }

  .button {
      height: 40px;
      padding: 0 35px;
      border-radius: 50px;
      display: inline-block;
      font-size: 12px;
      font-family: 'DejaVu Sans Mono', monospace;
      color: black;
      background-color: white;
  }
</style>
    """
  }

  fun generate(): String {
    return buildHtml {
      head {
        unsafe(CSS_STYLES)
      }

      body {
        h2 { +"Modify ignored problems here" }
        div {
          +"Ignoring lines must be in the form: <plugin_xml_id>:[<plugin_version>:]<problem_description_regexp_pattern>, for example:"
          br()
          +"    org.some.plugin:3.4.0:access to unresolved class org.foo.Foo.*                    --- ignore for plugin 'org.some.plugin' of version 3.4.0\""
          br()
          +"    org.jetbrains.kotlin::access to unresolved class org.jetbrains.kotlin.compiler.*  --- ignore for all versions of Kotlin plugin"
        }
        form("ignoredProblemsForm", "", "/info/modify-ignored-problems", method = "post") {
          textarea("ignored_problems_form", "ignoredProblemsForm", "ignored.problems", "Enter ignored problems here") {
            getIgnoredProblemsLines().forEach {
              +it
              +"\n"
            }
          }

          div("labeled_field") {
            div("label") { +"Admin password" }
            input("password", "admin.password", "", "label", title = "Admin password")
          }
          div {
            input("submit", classes = "button", value = "Modify", name = "submit")
          }
        }
      }
    }
  }

  private fun getIgnoredProblemsLines() = ignoreConditions.map { it.serializeCondition() }

}

/**
 * Builds HTML web page using provided builder [function].
 */
fun buildHtml(function: HtmlBuilder.() -> Unit): String {
  val stringWriter = StringWriter()
  val printWriter = PrintWriter(stringWriter)
  val htmlBuilder = HtmlBuilder(printWriter)
  htmlBuilder.html {
    htmlBuilder.function()
  }
  printWriter.close()
  return stringWriter.toString()
}
