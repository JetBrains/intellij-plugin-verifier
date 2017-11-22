package com.jetbrains.pluginverifier.repository.local.meta

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.local.LocalPluginInfo
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.bind.JAXBContext

/**
 * Created by Sergey.Patrikeev
 */
class LocalRepositoryMetadataParser {

  fun parseFromXml(xmlFile: Path): List<LocalPluginInfo> {
    val metadataBean = parseMetadataBean(Files.newInputStream(xmlFile))
    return metadataBean.categories.flatMap { categoryBean ->
      categoryBean.plugins.map { plugin ->
        LocalPluginInfo(
            plugin.id,
            plugin.version,
            plugin.name,
            IdeVersion.createIdeVersion(plugin.ideaVersion.sinceBuild),
            plugin.ideaVersion.untilBuild?.let { IdeVersion.createIdeVersion(it) },
            plugin.vendor.name,
            xmlFile.resolveSibling(plugin.downloadUrl),
            //todo: in fact, this is not defined in the plugins.xml. The only way to fetch this info is to read plugin descriptors.
            definedModules = emptySet()
        )
      }
    }
  }

  private fun parseMetadataBean(inputStream: InputStream): LocalRepositoryMetadataBean {
    val jaxbContext = JAXBContext.newInstance(LocalRepositoryMetadataBean::class.java)
    return jaxbContext.createUnmarshaller().unmarshal(inputStream) as LocalRepositoryMetadataBean
  }

}