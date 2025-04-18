package com.jetbrains.plugin.structure.xml

import com.jetbrains.plugin.structure.base.utils.closeAll
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.xml.stream.EventFilter
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamException
import javax.xml.stream.events.XMLEvent

private val LOG: Logger = LoggerFactory.getLogger(XmlStreamEventFilter::class.java)

class XmlStreamEventFilter {
  @Throws(IOException::class)
  fun filter(eventFilter: EventFilter, pluginXmlInputStream: InputStream, pluginXmlOutputStream: OutputStream, xmlTransformationContext: XmlTransformationContext) {
    val closeables = mutableListOf<Closeable>()
    try {
      val eventReader = xmlTransformationContext.xmlInputFactory
        .newFilteredEventReader(pluginXmlInputStream, eventFilter)
        .also { closeables += it }
      val eventWriter = newEventWriter(xmlTransformationContext.xmlOutputFactory, pluginXmlOutputStream).also { closeables += it }

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
      LOG.error("Cannot retrieve next event", e)
      false
    } catch (e: RuntimeException) {
      LOG.error("Cannot retrieve next event", e)
      false
    }
  }
}