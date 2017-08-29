package com.jetbrains.structure.teamcity.beans

import org.jdom2.Document
import org.jdom2.input.SAXBuilder
import org.jonnyzzz.kotlin.xml.bind.jdom.JDOM
import java.io.File

fun extractPluginBean(descriptorFile: File): TeamcityPluginBean {
  val rootElement = (SAXBuilder().build(descriptorFile) as Document).rootElement
  return JDOM.load(rootElement, TeamcityPluginBean::class.java)
}