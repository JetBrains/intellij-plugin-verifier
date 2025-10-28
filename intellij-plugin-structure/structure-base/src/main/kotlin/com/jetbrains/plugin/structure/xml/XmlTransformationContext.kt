package com.jetbrains.plugin.structure.xml

import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLOutputFactory

class XmlTransformationContext private constructor(val xmlInputFactory: XMLInputFactory, val xmlOutputFactory: XMLOutputFactory) {
  companion object {
    fun create(): XmlTransformationContext {
      val inputFactory = XMLInputFactory.newInstance().apply {
        setProperty(XMLInputFactory.SUPPORT_DTD, false)
        setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
      }
      return XmlTransformationContext(inputFactory, XMLOutputFactory.newInstance())
    }
  }
}