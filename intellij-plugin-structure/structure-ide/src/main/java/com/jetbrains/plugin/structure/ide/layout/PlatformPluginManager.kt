package com.jetbrains.plugin.structure.ide.layout

import com.jetbrains.plugin.structure.base.utils.listJars
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.ide.IdeManagerImpl.Companion.PLATFORM_PLUGIN_XML
import com.jetbrains.plugin.structure.ide.IntelliJPlatformProduct
import com.jetbrains.plugin.structure.ide.layout.PluginWithArtifactPathResult.Companion.logFailures
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.JarFilesResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.jar.META_INF
import com.jetbrains.plugin.structure.jar.PLUGIN_XML
import com.jetbrains.plugin.structure.jar.PluginDescriptorResult
import com.jetbrains.plugin.structure.jar.PluginJar
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path

private val LOG: Logger = LoggerFactory.getLogger(PlatformPluginManager::class.java)

internal class PlatformPluginManager(private val pluginLoader: LayoutComponentLoader) {
  fun loadPlatformPlugins(idePath: Path, ideVersion: IdeVersion): List<IdePlugin> {
    val platformJarFiles = idePath.resolve("lib").listJars()
    val platformResourceResolver = JarFilesResourceResolver(platformJarFiles)

    val loadPlugin = { jarPath: Path -> loadPlugin(jarPath, ideVersion, platformResourceResolver) }
    val loadedPlugins = platformJarFiles.mapNotNull(loadPlugin)
    val loadingResults = LoadingResults(loadedPlugins)
    logFailures(LOG, loadingResults.failures, idePath)
    return loadingResults.successfulPlugins
  }

  private fun loadPlugin(jarPath: Path, ideVersion: IdeVersion, platformResourceResolver: ResourceResolver): PluginWithArtifactPathResult? {
    return findDescriptor(jarPath, ideVersion)?.let { descriptor ->
      pluginLoader.load(jarPath, descriptor, platformResourceResolver, ideVersion)
    }
  }

  private fun findDescriptor(jarPath: Path, ideVersion: IdeVersion): String? =
    PluginJar(jarPath).use { pluginJar ->
      when (val pluginDescriptorResult = pluginJar.getPluginDescriptor(*ideVersion.descriptorPaths)) {
        is PluginDescriptorResult.Found -> pluginDescriptorResult.path.simpleName
        else -> null
      }
    }

  private val IdeVersion.descriptorPaths: Array<String>
    get() {
      operator fun String.div(fileName: String) = "$this${File.separator}$fileName"
      return arrayOf(
        META_INF / "${platformPrefix}Plugin.xml",
        META_INF / PLUGIN_XML,
        META_INF / PLATFORM_PLUGIN_XML
      )
    }

  private val IdeVersion.platformPrefix: String
    get() {
      val platformProduct = IntelliJPlatformProduct.fromIdeVersion(this)
      val product = platformProduct ?: IntelliJPlatformProduct.IDEA
      return product.platformPrefix
    }
}