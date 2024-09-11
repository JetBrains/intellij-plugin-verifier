/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.dotnet

import com.jetbrains.plugin.structure.base.decompress.DecompressorSizeLimitExceededException
import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.plugin.structure.dotnet.beans.ReSharperPluginBean
import com.jetbrains.plugin.structure.dotnet.beans.ReSharperPluginBeanExtractor
import com.jetbrains.plugin.structure.dotnet.problems.createIncorrectDotNetPluginFileProblem
import org.slf4j.LoggerFactory
import org.xml.sax.SAXParseException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Suppress("unused")
class ReSharperPluginManager private constructor(private val extractDirectory: Path) : PluginManager<ReSharperPlugin> {
  companion object {
    private val LOG = LoggerFactory.getLogger(ReSharperPluginManager::class.java)
    const val THIRD_PARTY_LIBRARIES_FILE_NAME = "dependencies.json"

    fun createManager(
      extractDirectory: Path = Paths.get(Settings.EXTRACT_DIRECTORY.get())
    ): ReSharperPluginManager {
      extractDirectory.createDir()
      return ReSharperPluginManager(extractDirectory)
    }
  }

  override fun createPlugin(pluginFile: Path): PluginCreationResult<ReSharperPlugin> {
    require(pluginFile.exists()) { "Plugin file $pluginFile does not exist" }
    return when (pluginFile.extension) {
      "nupkg" -> loadDescriptorFromNuPkg(pluginFile)
      else -> PluginCreationFail(createIncorrectDotNetPluginFileProblem(pluginFile.simpleName))
    }
  }

  private fun loadDescriptorFromNuPkg(pluginFile: Path): PluginCreationResult<ReSharperPlugin> {
    val sizeLimit = Settings.RE_SHARPER_PLUGIN_SIZE_LIMIT.getAsLong()
    if (Files.size(pluginFile) > sizeLimit) {
      return PluginCreationFail(PluginFileSizeIsTooLarge(sizeLimit))
    }
    val tempDirectory = Files.createTempDirectory(extractDirectory, "plugin_")
    return try {
      val extractedDirectory = tempDirectory.resolve("content")
      extractZip(pluginFile, extractedDirectory, sizeLimit)
      loadDescriptorFromDirectory(extractedDirectory)
    } catch (e: DecompressorSizeLimitExceededException) {
      return PluginCreationFail(PluginFileSizeIsTooLarge(e.sizeLimit))
    } catch (e: Exception) {
      return PluginCreationFail(UnableToExtractZip())
    } finally {
      tempDirectory.deleteLogged()
    }
  }

  private fun loadDescriptorFromDirectory(pluginDirectory: Path): PluginCreationResult<ReSharperPlugin> {
    val candidateDescriptors = pluginDirectory.listRecursivelyAllFilesWithExtension("nuspec")
    if (candidateDescriptors.isEmpty()) {
      return PluginCreationFail(PluginDescriptorIsNotFound("*.nuspec"))
    }
    val descriptorFile = candidateDescriptors.first()
    if (candidateDescriptors.size > 1) {
      return PluginCreationFail(
        MultiplePluginDescriptors(
          descriptorFile.fileName.toString(),
          "plugin.nupkg",
          candidateDescriptors[1].fileName.toString(),
          "plugin.nupkg"
        )
      )
    }
    val thirdPartyDependencies = resolveThirdPartyDependencies(pluginDirectory)
    return loadDescriptor(descriptorFile, thirdPartyDependencies)
  }

  private fun resolveThirdPartyDependencies(pluginDirectory: Path): List<ThirdPartyDependency> {

    val depsPath = pluginDirectory.resolve(THIRD_PARTY_LIBRARIES_FILE_NAME)
    return parseThirdPartyDependenciesByPath(depsPath)
  }

  private fun loadDescriptor(
    descriptorFile: Path,
    thirdPartyDependencies: List<ThirdPartyDependency>,
  ): PluginCreationResult<ReSharperPlugin> {
    try {
      val descriptorContent = Files.readAllBytes(descriptorFile)
      val bean = ReSharperPluginBeanExtractor.extractPluginBean(descriptorContent.inputStream())
      val beanValidationResult = validateDotNetPluginBean(bean)
      if (beanValidationResult.any { it.level == PluginProblem.Level.ERROR }) {
        return PluginCreationFail(beanValidationResult)
      }
      val plugin = createPluginFromValidBean(bean, descriptorContent, thirdPartyDependencies)
      return PluginCreationSuccess(plugin, beanValidationResult)
    } catch (e: SAXParseException) {
      return PluginCreationFail(UnexpectedDescriptorElements(e.lineNumber))
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      LOG.info("Unable to read plugin descriptor: ${descriptorFile.fileName}", e)
      return PluginCreationFail(UnableToReadDescriptor(descriptorFile.fileName.toString(), e.localizedMessage))
    }
  }

  private fun createPluginFromValidBean(bean: ReSharperPluginBean, nuspecFileContent: ByteArray, thirdPartyDependencies: List<ThirdPartyDependency>) = with(bean) {
    val id = this.id!!
    val idParts = id.split('.')
    val vendor = if (idParts.size > 1) idParts[0] else null
    val authors = authors!!.split(',').map { it.trim() }
    val pluginName = when {
      title != null -> title!!
      idParts.size > 1 -> idParts[1]
      else -> id
    }
    val nuspecFileName = "$id.nuspec"
    ReSharperPlugin(
      pluginId = id,
      pluginName = pluginName,
      vendor = vendor,
      nonNormalizedVersion = this.version!!,
      url = this.url,
      changeNotes = this.changeNotes,
      description = this.description,
      vendorEmail = null,
      vendorUrl = null,
      authors = authors,
      licenseUrl = licenseUrl,
      copyright = copyright,
      summary = summary,
      thirdPartyDependencies = thirdPartyDependencies,
      dependencies = getAllDependencies().map { DotNetDependency(it.id!!, it.version) },
      nuspecFile = PluginFile(nuspecFileName, nuspecFileContent)
    )
  }
}