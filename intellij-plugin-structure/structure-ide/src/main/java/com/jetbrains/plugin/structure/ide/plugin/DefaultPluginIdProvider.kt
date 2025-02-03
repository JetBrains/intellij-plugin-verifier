/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.plugin

import com.jetbrains.plugin.structure.intellij.plugin.PluginIdProvider
import com.jetbrains.plugin.structure.xml.ElementTextContentFilter
import com.jetbrains.plugin.structure.xml.EventTypeExcludingEventFilter
import com.jetbrains.plugin.structure.xml.LogicalAndXmlEventFilter
import com.jetbrains.plugin.structure.xml.XmlStreamEventFilter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.xml.stream.EventFilter
import javax.xml.stream.XMLStreamConstants.COMMENT
import javax.xml.stream.XMLStreamConstants.START_DOCUMENT

private const val pluginIdXPath = "/idea-plugin/id"

class DefaultPluginIdProvider : PluginIdProvider {
  private val xmlStreamEventFilter = XmlStreamEventFilter()

  @Throws(IOException::class)
  override fun getPluginId(pluginDescriptorStream: InputStream): String {
    val elementTextContentFilter = ElementTextContentFilter(pluginIdXPath)
    val eventFilter = mutableListOf<EventFilter>().apply {
      add(elementTextContentFilter)
      add(EventTypeExcludingEventFilter(START_DOCUMENT))
      add(EventTypeExcludingEventFilter(COMMENT))
    }
      .let { LogicalAndXmlEventFilter(it) }

    xmlStreamEventFilter.filter(eventFilter, pluginDescriptorStream, NullOutputStream)

    return elementTextContentFilter.value
  }

  private object NullOutputStream : OutputStream() {
    override fun write(b: Int) = Unit
  }
}