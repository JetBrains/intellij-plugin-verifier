package com.jetbrains.plugin.structure.ide.dependencies

import com.jetbrains.plugin.structure.base.utils.closeAll
import com.jetbrains.plugin.structure.xml.CloseableXmlEventReader
import com.jetbrains.plugin.structure.xml.CountingXmlEventWriter
import com.jetbrains.plugin.structure.xml.ElementNamesFilter
import com.jetbrains.plugin.structure.xml.EventTypeExcludingEventFilter
import com.jetbrains.plugin.structure.xml.LogicalAndXmlEventFilter
import com.jetbrains.plugin.structure.xml.newEventWriter
import com.jetbrains.plugin.structure.xml.newFilteredEventReader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.xml.stream.EventFilter
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamException
import javax.xml.stream.events.XMLEvent
import javax.xml.stream.events.XMLEvent.COMMENT
import javax.xml.stream.events.XMLEvent.START_DOCUMENT

private val LOG: Logger = LoggerFactory.getLogger(PluginXmlDependencyFilter::class.java)

class PluginXmlDependencyFilter(private val ignoreComments: Boolean = true, private val ignoreXmlDeclaration: Boolean = true) {
  @Throws(IOException::class)
  fun filter(pluginXmlInputStream: InputStream, pluginXmlOutputStream: OutputStream) {
    val closeables = mutableListOf<Closeable>()
    try {
      val inputFactory: XMLInputFactory = newXmlInputFactory()
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
      val eventWriter = newEventWriter(outputFactory, pluginXmlOutputStream).also { closeables += it }

      while (eventReader.hasNextEvent()) {
        val event: XMLEvent = eventReader.nextEvent()
        eventWriter.add(event)
      }

    } catch (e: Exception) {
      throw IOException("Cannot filter plugin descriptor input stream", e)
    } finally {
      closeables.closeAll()
    }
  }

  private fun newEventWriter(outputFactory: XMLOutputFactory, outputStream: OutputStream): CountingXmlEventWriter {
    return CountingXmlEventWriter(outputFactory.newEventWriter(outputStream))
  }

  private fun CloseableXmlEventReader.hasNextEvent(): Boolean {
    return try {
      hasNext()
    } catch (e: XMLStreamException) {
      LOG.atError().log("Cannot retrieve next event", e)
      false
    } catch (e: RuntimeException) {
      LOG.atError().log("Cannot retrieve next event", e)
      false
    }
  }

  private fun newXmlInputFactory() = XMLInputFactory.newInstance().apply {
    setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false)
    setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
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