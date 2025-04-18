/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.xml

import com.jetbrains.plugin.structure.xml.XmlInputFactoryResult.ConfigurationError
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import javax.xml.stream.EventFilter
import javax.xml.stream.FactoryConfigurationError
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLEventWriter
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamException

private val XML_EVENT_READER_LOG: Logger = LoggerFactory.getLogger(CloseableXmlEventReader::class.java)

sealed class XmlInputFactoryResult {
  data class Created(val xmlInputFactory: XMLInputFactory) : XmlInputFactoryResult()
  data class ConfigurationError(val t: Throwable) : XmlInputFactoryResult()
}

fun createXmlInputFactory(): XmlInputFactoryResult {
  try {
    val inputFactory = newXmlInputFactory()
    return XmlInputFactoryResult.Created(inputFactory)
  } catch (e: FactoryConfigurationError) {
    return ConfigurationError(e)
  } catch (e: IllegalArgumentException) {
    return ConfigurationError(e)
  }
}

fun newXmlInputFactory(): XMLInputFactory = XMLInputFactory.newInstance().apply {
  setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false)
  setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
}

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

  fun hasNextEvent(): Boolean {
    return try {
      hasNext()
    } catch (e: XMLStreamException) {
      XML_EVENT_READER_LOG.error("Cannot retrieve next event", e)
      false
    } catch (e: RuntimeException) {
      XML_EVENT_READER_LOG.error("Cannot retrieve next event", e)
      false
    }
  }
}

class CloseableXmlEventWriter(private val delegate: XMLEventWriter) : XMLEventWriter by delegate, Closeable {
  @Throws(XMLStreamException::class)
  override fun close() {
    delegate.close()
  }
}