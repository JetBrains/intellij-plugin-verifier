package com.jetbrains.plugin.structure.xml

import com.jetbrains.plugin.structure.ide.dependencies.PluginXmlDependencyFilter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import javax.xml.stream.EventFilter
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLEventWriter
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamReader
import javax.xml.stream.XMLStreamWriter
import javax.xml.stream.events.XMLEvent

fun XMLInputFactory.newEventReader(inputStream: InputStream): CloseableXmlEventReader =
  CloseableXmlEventReader(createXMLEventReader(inputStream))

fun XMLInputFactory.newFilteredEventReader(inputStream: InputStream, filter: EventFilter): CloseableXmlEventReader =
  CloseableXmlEventReader(createFilteredReader(newEventReader(inputStream), filter))

fun XMLOutputFactory.newEventWriter(outputStream: OutputStream): CloseableXmlEventWriter =
  CloseableXmlEventWriter(createXMLEventWriter(outputStream))

class CloseableXmlEventReader(private val delegate: XMLEventReader) : XMLEventReader by delegate, Closeable {
  @Throws(XMLStreamException::class)
  override fun close() {
    delegate.close()
  }
}

class CloseableXmlEventWriter(private val delegate: XMLEventWriter) : XMLEventWriter by delegate, Closeable {
  @Throws(XMLStreamException::class)
  override fun close() {
    delegate.close()
  }
}

class CountingXmlEventWriter(private val delegate: XMLEventWriter) : XMLEventWriter by delegate, Closeable {
  private val eventCounter = hashMapOf<XmlEventType, Int>()

  override fun add(event: XMLEvent) {
    delegate.add(event)
    val type = event.eventType
    eventCounter[type] = (eventCounter[type] ?: 0) + 1
  }

  @Throws(XMLStreamException::class)
  override fun close() {
    if ((eventCounter.size == 1 && eventCounter[XMLEvent.START_DOCUMENT] == 1) || eventCounter.isEmpty()) {
      // closing without an actual document being written
      try {
        delegate.close()
      } catch (e: Exception) {
        when (e) {
          is RuntimeException, is XMLStreamException -> {
            LOG.atError().log("Failed to close delegate XML event writer: {}", e.message)
            return
          }
        }
      }
    } else {
      delegate.close()
    }
  }

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(PluginXmlDependencyFilter::class.java)
  }
}

class CloseableXmlStreamReader(private val delegate: XMLStreamReader) : XMLStreamReader by delegate, Closeable {
  @Throws(XMLStreamException::class)
  override fun close() {
    delegate.close()
  }
}

class CloseableXmlStreamWriter(private val delegate: XMLStreamWriter) : XMLStreamWriter by delegate, Closeable {
  @Throws(XMLStreamException::class)
  override fun close() {
    delegate.close()
  }
}