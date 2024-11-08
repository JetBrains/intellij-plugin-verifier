/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.xml

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