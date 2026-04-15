/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.loaders

import com.jetbrains.plugin.structure.intellij.utils.JDOMUtil
import com.jetbrains.plugin.structure.jar.PluginDescriptorResult
import org.jdom2.Document
import java.io.InputStream

internal data class LoadedXml(
  val document: Document,
  val mayHaveXIncludes: Boolean,
)

private val XINCLUDE_NAMESPACE_MARKER = "http://www.w3.org/2001/XInclude".toByteArray()
private val XINCLUDE_ELEMENT_MARKER = ":include".toByteArray()

internal fun PluginDescriptorResult.Found.loadXml(): LoadedXml = inputStream.use {
  it.loadXml()
}

internal fun InputStream.loadXml(): LoadedXml {
  val bytes = readBytes()
  return LoadedXml(
    document = JDOMUtil.loadDocument(bytes.inputStream()),
    mayHaveXIncludes = bytes.contains(XINCLUDE_NAMESPACE_MARKER) || bytes.contains(XINCLUDE_ELEMENT_MARKER)
  )
}

private fun ByteArray.contains(fragment: ByteArray): Boolean {
  if (fragment.isEmpty() || size < fragment.size) {
    return false
  }
  for (start in 0..size - fragment.size) {
    var matches = true
    for (offset in fragment.indices) {
      if (this[start + offset] != fragment[offset]) {
        matches = false
        break
      }
    }
    if (matches) {
      return true
    }
  }
  return false
}
