package com.jetbrains.plugin.structure.xml

import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLOutputFactory

class XmlTransformationContext private constructor(val xmlInputFactory: XMLInputFactory, val xmlOutputFactory: XMLOutputFactory) {
  companion object {
    fun create(): XmlTransformationContext {
      return XmlTransformationContext(XMLInputFactory.newInstance(), XMLOutputFactory.newInstance())
    }
  }
}