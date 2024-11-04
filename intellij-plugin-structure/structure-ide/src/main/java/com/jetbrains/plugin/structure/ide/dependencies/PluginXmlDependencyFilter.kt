package com.jetbrains.plugin.structure.ide.dependencies

import com.jetbrains.plugin.structure.base.utils.closeAll
import com.jetbrains.plugin.structure.xml.ElementNamesFilter
import com.jetbrains.plugin.structure.xml.EventTypeExcludingEventFilter
import com.jetbrains.plugin.structure.xml.LogicalAndXmlEventFilter
import com.jetbrains.plugin.structure.xml.newEventWriter
import com.jetbrains.plugin.structure.xml.newFilteredEventReader
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.xml.stream.EventFilter
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.events.XMLEvent
import javax.xml.stream.events.XMLEvent.COMMENT
import javax.xml.stream.events.XMLEvent.START_DOCUMENT

class PluginXmlDependencyFilter(private val ignoreComments: Boolean = true, private val ignoreXmlDeclaration: Boolean = true) {
  @Throws(IOException::class)
  fun filter(pluginXmlInputStream: InputStream, pluginXmlOutputStream: OutputStream) {
    val closeables = mutableListOf<Closeable>()
    try {
      val inputFactory: XMLInputFactory = XMLInputFactory.newInstance()
      val outputFactory: XMLOutputFactory = XMLOutputFactory.newInstance()

      val elementNameFilter = ElementNamesFilter(
        "idea-plugin", "id", "depends", "dependencies", "plugin", "module", "/idea-plugin/dependencies/module")
      val eventFilter = mutableListOf<EventFilter>().apply {
        add(elementNameFilter)
        if (ignoreXmlDeclaration) add(EventTypeExcludingEventFilter(START_DOCUMENT))
        if (ignoreComments) add(EventTypeExcludingEventFilter(COMMENT))
      }.let { LogicalAndXmlEventFilter(it) }

      val eventReader = inputFactory
        .newFilteredEventReader(pluginXmlInputStream, eventFilter)
        .also { closeables += it }
      val eventWriter = outputFactory.newEventWriter(pluginXmlOutputStream).also { closeables += it }

      while (eventReader.hasNext()) {
        val event: XMLEvent = eventReader.nextEvent()
        eventWriter.add(event)
      }

    } catch (e: Exception) {
      throw IOException("Cannot filter plugin descriptor input stream", e)
    } finally {
      closeables.closeAll()
    }
  }
}