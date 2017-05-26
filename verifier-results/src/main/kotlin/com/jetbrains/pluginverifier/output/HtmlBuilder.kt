package com.jetbrains.pluginverifier.output

import com.google.common.html.HtmlEscapers
import java.io.PrintWriter

/**
 * @author Sergey Patrikeev
 */
class HtmlBuilder(val output: PrintWriter) {
  fun tag(tagName: String, block: () -> Unit = {}, attr: Map<String, String> = emptyMap()) {
    val open = if (attr.isEmpty()) {
      "<$tagName>"
    } else {
      "<$tagName " + attr.filterValues { it.isNotEmpty() }.entries.joinToString(separator = " ") { "${it.key}=\"${it.value}\"" } + ">"
    }
    output.println(open)
    block()
    output.println("</$tagName>")
  }

  fun html(block: () -> Unit) = tag("html", block)

  fun p(block: () -> Unit) = tag("p", block)

  fun br() {
    output.appendln("<br/>")
  }

  fun body(block: () -> Unit) = tag("body", block)

  fun head(block: () -> Unit) = tag("head", block)

  fun h2(block: () -> Unit) = tag("h2", block)

  fun h3(block: () -> Unit) = tag("h3", block)

  fun span(classes: String = "", block: () -> Unit) = tag("span", block, mapOf("class" to classes))

  fun label(block: () -> Unit) = tag("label", block)

  fun title(text: String) = tag("title", { output.println(text) })

  fun script(src: String = "", block: () -> Unit = {}) = tag("script", block, mapOf("src" to src))

  fun link(rel: String = "", href: String = "", block: () -> Unit = {}) = tag("link", block, mapOf("rel" to rel, "href" to href))

  fun unsafe(text: String) = output.println(text)

  fun div(classes: String = "", block: () -> Unit) = tag("div", block, mapOf("class" to classes))

  fun small(block: () -> Unit) = tag("small", block)

  fun a(href: String = "", classes: String = "", block: () -> Unit) = tag("a", block, mapOf("href" to href, "class" to classes))

  fun style(type: String = "", block: () -> Unit) = tag("style", block, mapOf("type" to type))

  operator fun String.unaryPlus() {
    output.append(HtmlEscapers.htmlEscaper().escape(this))
  }

}