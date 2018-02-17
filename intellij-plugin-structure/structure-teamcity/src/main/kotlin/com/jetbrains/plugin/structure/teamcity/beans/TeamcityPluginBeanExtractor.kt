package com.jetbrains.plugin.structure.teamcity.beans

import org.jdom2.input.SAXBuilder
import org.jonnyzzz.kotlin.xml.bind.jdom.JDOM
import org.xml.sax.InputSource
import java.io.CharArrayReader
import java.io.InputStream

private val EMPTY_CHAR_ARRAY = CharArray(0)

fun extractPluginBean(inputStream: InputStream): TeamcityPluginBean {
  val saxBuilder = SAXBuilder()
  saxBuilder.setEntityResolver({ _, _ -> InputSource(CharArrayReader(EMPTY_CHAR_ARRAY)) })
  val rootElement = saxBuilder.build(inputStream).rootElement
  return JDOM.load(rootElement, TeamcityPluginBean::class.java)
}