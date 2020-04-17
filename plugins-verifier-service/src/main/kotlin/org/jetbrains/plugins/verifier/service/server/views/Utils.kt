/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.server.views

import com.jetbrains.pluginverifier.misc.HtmlBuilder
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter

/**
 * Builds HTML web page using provided builder [function].
 */
fun OutputStream.buildHtml(function: HtmlBuilder.() -> Unit) {
  val stringWriter = OutputStreamWriter(this)
  val printWriter = PrintWriter(stringWriter)
  val htmlBuilder = HtmlBuilder(printWriter)
  htmlBuilder.html {
    htmlBuilder.function()
  }
  printWriter.close()
}