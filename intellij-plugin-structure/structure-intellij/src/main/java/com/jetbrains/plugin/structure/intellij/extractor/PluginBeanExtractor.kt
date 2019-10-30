package com.jetbrains.plugin.structure.intellij.extractor

import com.jetbrains.plugin.structure.intellij.beans.PluginBean
import org.jdom2.Document
import org.jdom2.transform.JDOMSource
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException

object PluginBeanExtractor {

  private val jaxbContext by lazy {
    /**
     * Explicitly specify class loader for loading implementation classes.
     *
     * It is necessary for applications that have complex hierarchy of class loaders.
     * If we don't specify the class loader, a thread's context class loader would be used,
     * which may not found necessary classes in some scenarios.
     *
     * JAXB has only this option to pass class loader. Resource file 'jaxb.index' is used
     * to specify top-level classes.
     */
    JAXBContext.newInstance("com.jetbrains.plugin.structure.intellij.beans", PluginBeanExtractor.javaClass.classLoader)
  }

  @Throws(JAXBException::class)
  fun extractPluginBean(document: Document): PluginBean {
    val unmarshaller = jaxbContext.createUnmarshaller()
    return unmarshaller.unmarshal(JDOMSource(document)) as PluginBean
  }

}
