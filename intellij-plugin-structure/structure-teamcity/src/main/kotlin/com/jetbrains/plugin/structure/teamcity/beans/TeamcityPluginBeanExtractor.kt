/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.teamcity.beans

import com.jetbrains.plugin.structure.xml.DefaultXMLDocumentBuilderProvider
import org.xml.sax.SAXParseException
import java.io.InputStream
import javax.xml.bind.JAXBContext
import javax.xml.bind.UnmarshalException

object TeamcityPluginBeanExtractor {
  private val jaxbContext = JAXBContext.newInstance(TeamcityPluginBean::class.java)

  @Throws(UnmarshalException::class, SAXParseException::class)
  fun extractPluginBean(inputStream: InputStream): TeamcityPluginBean {
    val document = DefaultXMLDocumentBuilderProvider.documentBuilder().parse(inputStream)
    val unmarshaller = jaxbContext.createUnmarshaller()
    return unmarshaller.unmarshal(document, TeamcityPluginBean::class.java).value
      ?: throw UnmarshalException("Metadata element not found")
  }
}