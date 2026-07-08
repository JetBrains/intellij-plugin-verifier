/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.misc

import java.io.Writer

@Suppress("unused")
class HtmlBuilder(val output: Writer) {
  private var style: TagStyle = TagStyle.MULTI_LINE
  private var indent: Int = 0
  private var noNextIndentNeeded: Boolean = false

  fun indent() {
    if (!noNextIndentNeeded) {
      repeat(indent) { output.write(' '.code) }
    }
    noNextIndentNeeded = false
  }

  private inline fun withStyle(style: TagStyle, function: () -> Unit) {
    if (style == TagStyle.INHERIT || style == this.style) {
      function()
      return
    }
    val oldStyle = this.style
    this.style = style
    try {
      function()
    } finally {
      this.style = oldStyle
    }
  }

  enum class TagStyle {
    SINGLE_LINE,
    MULTI_LINE,
    INHERIT,
  }

  fun tag(
    tagName: String,
    block: () -> Unit = {},
    attr: Map<String, String> = emptyMap(),
    style: TagStyle = TagStyle.INHERIT,
  ) {
    if (this.style == TagStyle.SINGLE_LINE && style == TagStyle.MULTI_LINE) {
      error("Cannot place multi-line tag $tagName in single-line one")
    }
    if (this.style != TagStyle.SINGLE_LINE) {
      indent()
    }
    output.write('<'.code)
    output.write(tagName)
    printAttributes(attr)
    output.write('>'.code)
    if (style.effective == TagStyle.MULTI_LINE) {
      output.write(System.lineSeparator())
    }
    indent += 2
    try {
      withStyle(style) {
        block()
      }
    } finally {
      indent -= 2
    }
    if (style.effective == TagStyle.MULTI_LINE) {
      indent()
    }
    output.write('<'.code)
    output.write('/'.code)
    output.write(tagName)
    output.write('>'.code)
    if (this.style == TagStyle.MULTI_LINE) {
      output.write(System.lineSeparator())
    }
    noNextIndentNeeded = false
  }

  private val TagStyle.effective: TagStyle
    get() = when (this) {
      TagStyle.INHERIT -> style
      else -> this
    }

  private fun printAttributes(attr: Map<String, String>) {
    for ((key, value) in attr) {
      if (value.isNotEmpty()) {
        output.write(' '.code)
        output.write(key)
        output.write('='.code)
        output.write('"'.code)
        output.write(value)
        output.write('"'.code)
      }
    }
  }

  fun closedTag(tagName: String, attr: Map<String, String> = emptyMap()) {
    if (this.style != TagStyle.SINGLE_LINE) {
      indent()
    }
    output.write('<'.code)
    output.write(tagName)
    printAttributes(attr)
    output.write('/'.code)
    output.write('>'.code)
    if (this.style == TagStyle.MULTI_LINE) {
      output.write(System.lineSeparator())
    }
    noNextIndentNeeded = false
  }

  fun html(block: () -> Unit) = tag("html", block)

  fun p(block: () -> Unit) = tag("p", block)

  fun ul(block: () -> Unit) = tag("ul", block)

  fun table(style: String, block: () -> Unit) = tag("table", block, mapOf("style" to style))

  fun tr(block: () -> Unit) = tag("tr", block)

  fun pre(block: () -> Unit) = tag("pre", block)

  fun th(style: String = "", block: () -> Unit) = tag("th", block, mapOf("style" to style))

  fun td(block: () -> Unit) = tag("td", block)

  fun li(block: () -> Unit) = tag("li", block, style = TagStyle.SINGLE_LINE)

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

  fun h1(block: () -> Unit) = tag("h1", block, style = TagStyle.SINGLE_LINE)

  fun h2(block: () -> Unit) = tag("h2", block, style = TagStyle.SINGLE_LINE)

  fun h3(block: () -> Unit) = tag("h3", block, style = TagStyle.SINGLE_LINE)

  fun span(classes: String = "", style: TagStyle = TagStyle.INHERIT, block: () -> Unit) = tag("span", block, mapOf("class" to classes), style = style)

  fun label(block: () -> Unit) = tag("label", block)

  fun title(text: String) = tag("title", { output.write(text) }, style = TagStyle.SINGLE_LINE)

  fun script(src: String = "", type: String = "", block: () -> Unit = {}) = tag("script", block, mapOf("src" to src, "type" to type))

  fun link(rel: String = "", href: String = "", type: String = "") = closedTag("link", mapOf("rel" to rel, "href" to href, "type" to type))

  fun unsafe(text: String) {
    output.write(text)
    output.write(System.lineSeparator())
  }

  fun div(classes: String = "", block: () -> Unit) = tag("div", block, mapOf("class" to classes))

  fun small(style: TagStyle = TagStyle.INHERIT, block: () -> Unit) = tag("small", block, style = style)

  fun a(href: String = "", classes: String = "", block: () -> Unit) = tag("a", block, mapOf("href" to href, "class" to classes), style = TagStyle.SINGLE_LINE)

  fun style(type: String = "", block: () -> Unit) = tag("style", block, mapOf("type" to type))

  operator fun String.unaryPlus() {
    this.escapeHtml4To(output)
    noNextIndentNeeded = !this.contains('\n')
  }

}