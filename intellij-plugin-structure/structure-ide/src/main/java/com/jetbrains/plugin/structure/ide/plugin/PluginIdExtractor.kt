package com.jetbrains.plugin.structure.ide.plugin

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

class PluginIdExtractor {
  private val xmlStreamEventFilter = XmlStreamEventFilter()

  @Throws(IOException::class)
  fun extractId(pluginXmlInputStream: InputStream): String {
    val elementTextContentFilter = ElementTextContentFilter(pluginIdXPath)
    val eventFilter = mutableListOf<EventFilter>().apply {
      add(elementTextContentFilter)
      add(EventTypeExcludingEventFilter(START_DOCUMENT))
      add(EventTypeExcludingEventFilter(COMMENT))
    }
      .let { LogicalAndXmlEventFilter(it) }

    xmlStreamEventFilter.filter(eventFilter, pluginXmlInputStream, NullOutputStream)

    return elementTextContentFilter.value
  }

  private object NullOutputStream : OutputStream() {
    override fun write(b: Int) = Unit
  }
}