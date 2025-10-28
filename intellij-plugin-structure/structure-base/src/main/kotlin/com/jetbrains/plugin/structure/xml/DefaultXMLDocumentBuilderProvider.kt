/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.xml

import org.apache.commons.lang3.ArrayUtils.EMPTY_CHAR_ARRAY
import org.slf4j.LoggerFactory
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
import org.xml.sax.helpers.DefaultHandler
import java.io.CharArrayReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

object DefaultXMLDocumentBuilderProvider {
  private val log = LoggerFactory.getLogger(DefaultXMLDocumentBuilderProvider::class.java)

  fun documentBuilder(): DocumentBuilder = DocumentBuilderFactory.newInstance().apply {
    isXIncludeAware = false
    isExpandEntityReferences = false
    setFeatureWithCatching(XMLParserConfiguration.FEATURE_DISALLOW_DOCTYPE_DECL, true)
    setFeatureWithCatching(XMLConstants.FEATURE_SECURE_PROCESSING, true)
    setFeatureWithCatching(XMLParserConfiguration.FEATURE_EXTERNAL_GENERAL_ENTITIES, false)
    setFeatureWithCatching(XMLParserConfiguration.FEATURE_EXTERNAL_PARAMETER_ENTITIES, false)
    setFeatureWithCatching(XMLParserConfiguration.FEATURE_LOAD_EXTERNAL_DTD, false)

    setAttributeWithCatching(XMLConstants.ACCESS_EXTERNAL_DTD, "")
    setAttributeWithCatching(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
  }.newDocumentBuilder().apply {
    setEntityResolver { _, _ -> InputSource(CharArrayReader(EMPTY_CHAR_ARRAY)) }
    setErrorHandler(object : DefaultHandler() {
      override fun error(e: SAXParseException) {
        throw e
      }
    })
  }

  private fun DocumentBuilderFactory.setFeatureWithCatching(name: String, value: Boolean) = runCatching {
    setFeature(name, value)
  }.onFailure {
    log.error("Failed to set feature $name to $value for default DocumentBuilderFactory", it)
  }.getOrNull()

  private fun DocumentBuilderFactory.setAttributeWithCatching(name: String, value: Any) = runCatching {
    setAttribute(name, value)
  }.onFailure {
    log.error("Failed to set attribute $name to $value for default DocumentBuilderFactory", it)
  }.getOrNull()
}
