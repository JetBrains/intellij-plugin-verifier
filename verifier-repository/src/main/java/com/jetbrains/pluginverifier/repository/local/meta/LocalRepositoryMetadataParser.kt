package com.jetbrains.pluginverifier.repository.local.meta

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.local.LocalPluginInfo
import java.io.File
import java.io.InputStream
import javax.xml.bind.JAXBContext

/**
 * Created by Sergey.Patrikeev
 */
class LocalRepositoryMetadataParser {

  fun parseFromXml(xmlFile: File): List<LocalPluginInfo> {
    val metadataBean = parseMetadataBean(xmlFile.inputStream())
    return metadataBean.categories.flatMap { categoryBean ->
      categoryBean.plugins.map { plugin ->
        LocalPluginInfo(
            plugin.id,
            plugin.version,
            plugin.name,
            IdeVersion.createIdeVersion(plugin.ideaVersion.sinceBuild),
            plugin.ideaVersion.untilBuild?.let { IdeVersion.createIdeVersion(it) },
            plugin.vendor.name,
            xmlFile.resolveSibling(plugin.downloadUrl)
        )
      }
    }
  }

  private fun parseMetadataBean(inputStream: InputStream): LocalRepositoryMetadataBean {
    val jaxbContext = JAXBContext.newInstance(LocalRepositoryMetadataBean::class.java)
    return jaxbContext.createUnmarshaller().unmarshal(inputStream) as LocalRepositoryMetadataBean
  }

}