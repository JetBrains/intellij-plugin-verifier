package com.jetbrains.plugin.structure.intellij.platform

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.hasExtension
import com.jetbrains.plugin.structure.intellij.beans.ModuleBean
import com.jetbrains.plugin.structure.intellij.extractor.ModuleUnmarshaller
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.bind.JAXBException
import kotlin.streams.asSequence

private val LOG: Logger = LoggerFactory.getLogger(BundledModulesResolver::class.java)

private const val MODULES_DIR = "modules"
private const val MODULE_DESCRIPTORS_JAR= "module-descriptors.jar"

class BundledModulesResolver(val idePath: Path, private val fileSystemProvider: JarFileSystemProvider) : ModulesResolver {

  private val moduleDescriptorsJarPath: Path = idePath.resolve(MODULES_DIR).resolve(MODULE_DESCRIPTORS_JAR)

  init {
    if (!moduleDescriptorsJarPath.exists()) {
      throw InvalidIdeException("IDE path [$idePath] does not contain '$MODULES_DIR/$MODULE_DESCRIPTORS_JAR' file")
    }
  }

  override fun resolveModules(): List<ModuleBean> {
    return fileSystemProvider.getFileSystem(moduleDescriptorsJarPath).use { jarFs ->
      val root: Path = jarFs.rootDirectories.first()
      Files.list(root).use { files ->
        files.asSequence()
          .filter { it.hasExtension("xml") }
          .mapNotNull(::unmarshallModule)
          .toList()
      }
    }
  }

  private fun unmarshallModule(xmlPath: Path): ModuleBean? {
    try {
      return ModuleUnmarshaller.unmarshall(xmlPath)
    } catch (e: JAXBException) {
      LOG.debug("Cannot unmarshall [{}/{}]: {}", moduleDescriptorsJarPath, xmlPath, e.message)
      return null
    }
  }
}