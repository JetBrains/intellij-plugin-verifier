package com.jetbrains.plugin.structure.intellij.extractor

import com.jetbrains.plugin.structure.base.utils.inputStream
import com.jetbrains.plugin.structure.intellij.beans.ModuleBean
import java.io.StringReader
import java.nio.file.Path
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import javax.xml.bind.Unmarshaller

object ModuleUnmarshaller {
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
    JAXBContext.newInstance("com.jetbrains.plugin.structure.intellij.beans", ModuleUnmarshaller.javaClass.classLoader)
  }

  @Throws(JAXBException::class)
  fun unmarshall(path: Path): ModuleBean {
    path.inputStream().use {
      return unmarshaller.unmarshal(it) as ModuleBean
    }
  }

  @Throws(JAXBException::class)
  fun unmarshall(xmlContent: String): ModuleBean {
    StringReader(xmlContent).use {
      return unmarshaller.unmarshal(it) as ModuleBean
    }
  }

  private val unmarshaller: Unmarshaller
    @Throws(JAXBException::class)
    get() = jaxbContext.createUnmarshaller()
}