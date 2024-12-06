/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.dependencies

import com.jetbrains.plugin.structure.xml.DocumentTypeFilter
import com.jetbrains.plugin.structure.xml.ElementNamesFilter
import com.jetbrains.plugin.structure.xml.EventTypeExcludingEventFilter
import com.jetbrains.plugin.structure.xml.LogicalAndXmlEventFilter
import com.jetbrains.plugin.structure.xml.XmlStreamEventFilter
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.xml.stream.EventFilter
import javax.xml.stream.events.XMLEvent.COMMENT
import javax.xml.stream.events.XMLEvent.START_DOCUMENT

class PluginXmlDependencyFilter(private val ignoreComments: Boolean = true, private val ignoreXmlDeclaration: Boolean = true) {
  private val allowedElements = listOf("idea-plugin", "id", "name", "vendor", "depends", "dependencies", "plugin",
    "module", "content", "/idea-plugin/dependencies/module", "/idea-plugin/content/module")

  private val passThruElements = setOf("module")

  private val xmlStreamEventFilter = XmlStreamEventFilter()

  @Throws(IOException::class)
  fun filter(pluginXmlInputStream: InputStream, pluginXmlOutputStream: OutputStream) {
    val eventFilter = mutableListOf<EventFilter>().apply {
      add(ElementNamesFilter(allowedElements))
      if (ignoreXmlDeclaration) add(EventTypeExcludingEventFilter(START_DOCUMENT))
      if (ignoreComments) add(EventTypeExcludingEventFilter(COMMENT))
    }
      .let { LogicalAndXmlEventFilter(it) }
      .let { DocumentTypeFilter(passThruElements, it) }

    return xmlStreamEventFilter.filter(eventFilter, pluginXmlInputStream, pluginXmlOutputStream)
  }

  companion object {
    fun PluginXmlDependencyFilter.toByteArray(pluginXmlInputStream: InputStream): ByteArray {
      return ByteArrayOutputStream().use {
        filter(pluginXmlInputStream, it)
        it.toByteArray()
      }
    }
  }
}