/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.xml

import com.jetbrains.plugin.structure.ide.dependencies.PluginXmlDependencyFilter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import javax.xml.stream.XMLEventWriter
import javax.xml.stream.XMLStreamException
import javax.xml.stream.events.XMLEvent

private val LOG: Logger = LoggerFactory.getLogger(PluginXmlDependencyFilter::class.java)

/**
 * STaX Event Writer that counts occurrences of STaX events.
 * It handles various peculiarities of underlying STaX implementations that prevent correct filtering
 * of semi-well-formed documents in the Platform.
 */
class CountingXmlEventWriter(private val delegate: XMLEventWriter) : XMLEventWriter by delegate, Closeable {
  private val eventCounter = hashMapOf<XmlEventType, Int>()

  override fun add(event: XMLEvent) {
    delegate.add(event)
    val type = event.eventType
    eventCounter[type] = (eventCounter[type] ?: 0) + 1
  }

  private fun count(type: XmlEventType) = eventCounter[type] ?: 0

  private fun processingInstructions(): Int {
    return count(XMLEvent.PROCESSING_INSTRUCTION)
  }

  private fun startDocuments(): Int {
    return count(XMLEvent.START_DOCUMENT)
  }

  @Throws(XMLStreamException::class)
  override fun close() {
    if ((eventCounter.size == 1 && startDocuments() == 1)
      || (eventCounter.size == 1 && processingInstructions() > 0)
      || eventCounter.isEmpty()) {
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
      try {
        delegate.close()
      } catch (e: Exception) {
        throw e
      }
    }
  }
}