package com.jetbrains.plugin.structure.teamcity.beans

import org.jdom2.Document
import org.jdom2.input.SAXBuilder
import org.jonnyzzz.kotlin.xml.bind.jdom.JDOM
import java.io.InputStream

fun extractPluginBean(descriptor: InputStream): TeamcityPluginBean {
  val rootElement = (SAXBuilder().build(descriptor) as Document).rootElement
  return JDOM.load(rootElement, TeamcityPluginBean::class.java)
}