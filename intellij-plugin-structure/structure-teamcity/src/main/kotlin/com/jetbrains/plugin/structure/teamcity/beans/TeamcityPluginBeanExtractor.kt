/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.teamcity.beans

import java.io.InputStream
import javax.xml.bind.JAXBContext
import javax.xml.bind.UnmarshalException
import javax.xml.parsers.DocumentBuilderFactory

object TeamcityPluginBeanExtractor {
  private val jaxbContext = JAXBContext.newInstance(TeamcityPluginBean::class.java)
  private val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

  @Throws(UnmarshalException::class)
  fun extractPluginBean(inputStream: InputStream): TeamcityPluginBean {
    val document = documentBuilder.parse(inputStream)
    val unmarshaller = jaxbContext.createUnmarshaller()
    return unmarshaller.unmarshal(document, TeamcityPluginBean::class.java).value
        ?: throw UnmarshalException("Metadata element not found")
  }
}