package com.jetbrains.pluginverifier.output

import com.google.common.html.HtmlEscapers
import java.io.PrintWriter

/**
 * @author Sergey Patrikeev
 */
class HtmlBuilder(val output: PrintWriter) {
  fun tag(tagName: String, block: () -> Unit = {}, attr: Map<String, String> = emptyMap()) {
    val renderedAttributes = renderAttributes(attr)
    val open = if (renderedAttributes.isEmpty()) {
      "<$tagName>"
    } else {
      "<$tagName $renderedAttributes>"
    }
    output.println(open)
    block()
    output.println("</$tagName>")
  }

  private fun renderAttributes(attr: Map<String, String>) =
      attr.filterValues { it.isNotEmpty() }.entries.joinToString(separator = " ") { "${it.key}=\"${it.value}\"" }

  fun closedTag(tagName: String, attr: Map<String, String> = emptyMap()) {
    val renderedAttributes = renderAttributes(attr)
    output.println(if (renderedAttributes.isEmpty()) {
      "<$tagName/>"
    } else {
      "<$tagName $renderedAttributes/>"
    })
  }

  fun html(block: () -> Unit) = tag("html", block)

  fun p(block: () -> Unit) = tag("p", block)

  fun ul(block: () -> Unit) = tag("ul", block)

  fun li(block: () -> Unit) = tag("li", block)

  fun br() = closedTag("br")

  fun body(block: () -> Unit) = tag("body", block)

  fun head(block: () -> Unit) = tag("head", block)

  fun h2(block: () -> Unit) = tag("h2", block)

  fun h3(block: () -> Unit) = tag("h3", block)

  fun span(classes: String = "", block: () -> Unit) = tag("span", block, mapOf("class" to classes))

  fun label(block: () -> Unit) = tag("label", block)

  fun title(text: String) = tag("title", { output.println(text) })

  fun script(src: String = "", type: String = "", block: () -> Unit = {}) = tag("script", block, mapOf("src" to src, "type" to type))

  fun link(rel: String = "", href: String = "", type: String = "") = closedTag("link", mapOf("rel" to rel, "href" to href, "type" to type))

  fun unsafe(text: String) = output.println(text)

  fun div(classes: String = "", block: () -> Unit) = tag("div", block, mapOf("class" to classes))

  fun small(block: () -> Unit) = tag("small", block)

  fun a(href: String = "", classes: String = "", block: () -> Unit) = tag("a", block, mapOf("href" to href, "class" to classes))

  fun style(type: String = "", block: () -> Unit) = tag("style", block, mapOf("type" to type))

  operator fun String.unaryPlus() {
    output.append(HtmlEscapers.htmlEscaper().escape(this))
  }

}