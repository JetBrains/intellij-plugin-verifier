/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.misc

import java.io.PrintWriter

class HtmlBuilder(val output: PrintWriter) {
  fun tag(tagName: String, block: () -> Unit = {}, attr: Map<String, String> = emptyMap()) {
    val open = if (attr.isEmpty()) {
      "<$tagName>"
    } else {
      "<$tagName ${renderAttributes(attr)}>"
    }
    output.println(open)
    block()
    output.println("</$tagName>")
  }

  private fun renderAttributes(attr: Map<String, String>) =
    attr.asSequence()
      .filter { it.value.isNotEmpty() }
      .joinToString(separator = " ") { "${it.key}=\"${it.value}\"" }

  fun closedTag(tagName: String, attr: Map<String, String> = emptyMap()) {
    output.println(
      if (attr.isEmpty()) {
        "<$tagName/>"
      } else {
        "<$tagName ${renderAttributes(attr)}/>"
      }
    )
  }

  fun html(block: () -> Unit) = tag("html", block)

  fun p(block: () -> Unit) = tag("p", block)

  fun ul(block: () -> Unit) = tag("ul", block)

  fun table(style: String, block: () -> Unit) = tag("table", block, mapOf("style" to style))

  fun tr(block: () -> Unit) = tag("tr", block)

  fun pre(block: () -> Unit) = tag("pre", block)

  fun th(style: String = "", block: () -> Unit) = tag("th", block, mapOf("style" to style))

  fun td(block: () -> Unit) = tag("td", block)

  fun li(block: () -> Unit) = tag("li", block)

  fun form(
    id: String,
    style: String = "",
    action: String,
    classes: String = "",
    method: String = "get",
    block: () -> Unit
  ) = tag(
    "form",
    block,
    mapOf(
      "action" to action,
      "id" to id,
      "style" to style,
      "class" to classes,
      "method" to method
    )
  )

  fun textarea(
    classes: String,
    form: String,
    name: String,
    title: String,
    block: () -> Unit
  ) =
    tag(
      "textarea",
      block,
      mapOf(
        "class" to classes,
        "form" to form,
        "name" to name,
        "title" to title
      )
    )

  fun input(
    type: String,
    name: String,
    value: String? = null,
    classes: String = "",
    form: String? = null,
    title: String = name
  ) {
    val tags = mutableMapOf("type" to type, "name" to name, "class" to classes, "title" to title)
    if (form != null) {
      tags["form"] = form
    }
    if (value != null) {
      tags["value"] = value
    }
    closedTag("input", tags)
  }

  fun br() = closedTag("br")

  fun body(block: () -> Unit) = tag("body", block)

  fun head(block: () -> Unit) = tag("head", block)

  fun h1(block: () -> Unit) = tag("h1", block)

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
    output.append(this.escapeHtml4())
  }

}