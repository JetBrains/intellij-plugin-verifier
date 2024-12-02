/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.xml

import com.jetbrains.plugin.structure.base.utils.closeAll
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.xml.stream.EventFilter
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamException
import javax.xml.stream.events.XMLEvent

private val LOG: Logger = LoggerFactory.getLogger(XmlFilter::class.java)

class XmlFilter {
  @Throws(IOException::class)
  fun filter(eventFilter: EventFilter, pluginXmlInputStream: InputStream, pluginXmlOutputStream: OutputStream) {
    val closeables = mutableListOf<Closeable>()
    try {
      val inputFactory: XMLInputFactory = newXmlInputFactory()
      val outputFactory: XMLOutputFactory = XMLOutputFactory.newInstance()

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
      LOG.error("Cannot retrieve next event", e)
      false
    } catch (e: RuntimeException) {
      LOG.error("Cannot retrieve next event", e)
      false
    }
  }

  private fun newXmlInputFactory() = XMLInputFactory.newInstance().apply {
    setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false)
    setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
  }
}