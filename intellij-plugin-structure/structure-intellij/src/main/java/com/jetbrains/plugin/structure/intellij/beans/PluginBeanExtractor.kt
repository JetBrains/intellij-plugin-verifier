package com.jetbrains.plugin.structure.intellij.beans

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.transform.JDOMSource
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException

object PluginBeanExtractor {

  private val jaxbContext by lazy {
    JAXBContext.newInstance(PluginBean::class.java)
  }

  @Throws(JAXBException::class)
  fun extractPluginBean(document: Document): PluginBean {
    val unmarshaller = jaxbContext.createUnmarshaller()

    val rootElement = document.rootElement
    val bean = unmarshaller.unmarshal(JDOMSource(document)) as PluginBean
    bean.extensions = extractExtensions(rootElement)
    return bean
  }

  private fun extractExtensions(rootElement: Element): Multimap<String, Element> {
    val extensions = ArrayListMultimap.create<String, Element>()
    for (extensionsRoot in rootElement.getChildren("extensions")) {
      for (element in extensionsRoot.children) {
        extensions.put(extractEPName(element), element)
      }
    }
    return extensions
  }

  private fun extractEPName(extensionElement: Element): String {
    var epName: String? = extensionElement.getAttributeValue("point")

    if (epName == null) {
      val parentElement = extensionElement.parentElement
      val ns = parentElement?.getAttributeValue("defaultExtensionNs")

      if (ns != null) {
        epName = ns + '.'.toString() + extensionElement.name
      } else {
        val namespace = extensionElement.namespace
        epName = namespace.uri + '.'.toString() + extensionElement.name
      }
    }
    return epName
  }

}
