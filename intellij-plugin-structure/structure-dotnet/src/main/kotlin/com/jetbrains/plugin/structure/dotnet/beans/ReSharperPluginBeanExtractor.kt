/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.dotnet.beans

import org.xml.sax.SAXParseException
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import javax.xml.bind.JAXBContext
import javax.xml.bind.UnmarshalException
import javax.xml.parsers.DocumentBuilderFactory


object ReSharperPluginBeanExtractor {
  private val jaxbContext = JAXBContext.newInstance(NuspecDocumentBean::class.java)
  private val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder().apply {
    setErrorHandler(object : DefaultHandler() {
      override fun error(e: SAXParseException) {
        throw e
      }
    })
  }

  @Throws(UnmarshalException::class)
  fun extractPluginBean(inputStream: InputStream): ReSharperPluginBean {
    val document = documentBuilder.parse(inputStream)
    val unmarshaller = jaxbContext.createUnmarshaller()
    return unmarshaller.unmarshal(document, NuspecDocumentBean::class.java).value.metadata
        ?: throw UnmarshalException("Metadata element not found")
  }
}
