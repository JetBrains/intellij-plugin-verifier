/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.dotnet.beans

import com.jetbrains.plugin.structure.xml.DefaultXMLDocumentBuilderProvider
import org.xml.sax.SAXParseException
import java.io.InputStream
import javax.xml.bind.JAXBContext
import javax.xml.bind.UnmarshalException


object ReSharperPluginBeanExtractor {
  private val jaxbContext = JAXBContext.newInstance(NuspecDocumentBean::class.java)

  @Throws(UnmarshalException::class, SAXParseException::class)
  fun extractPluginBean(inputStream: InputStream): ReSharperPluginBean {
    val document = DefaultXMLDocumentBuilderProvider.documentBuilder().parse(inputStream)
    val unmarshaller = jaxbContext.createUnmarshaller()
    return unmarshaller.unmarshal(document, NuspecDocumentBean::class.java).value.metadata
        ?: throw UnmarshalException("Metadata element not found")
  }
}
