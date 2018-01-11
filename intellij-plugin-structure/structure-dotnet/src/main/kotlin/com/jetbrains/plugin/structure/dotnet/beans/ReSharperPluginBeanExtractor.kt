package com.jetbrains.plugin.structure.dotnet.beans

import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.input.SAXBuilder
import org.jonnyzzz.kotlin.xml.bind.jdom.JDOM
import org.xml.sax.InputSource
import java.io.CharArrayReader
import java.io.InputStream

private val EMPTY_CHAR_ARRAY = CharArray(0)

fun extractPluginBean(inputStream: InputStream): ReSharperPluginBean {
  val builder = SAXBuilder()
  builder.setEntityResolver({ _, _ -> InputSource(CharArrayReader(EMPTY_CHAR_ARRAY)) })
  val rootElement = builder.build(inputStream).rootElement
  rootElement.namespace = Namespace.NO_NAMESPACE
  rootElement.descendants.forEach {
    if(it is Element) {
      it.namespace = Namespace.NO_NAMESPACE
    }
  }
  return JDOM.load(rootElement, ReSharperPluginBean::class.java)
}