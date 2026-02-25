package com.jetbrains.plugin.structure.ide.layout

import com.jetbrains.plugin.structure.base.utils.listJars
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.ide.IntelliJPlatformProduct
import com.jetbrains.plugin.structure.ide.InvalidIdeException
import com.jetbrains.plugin.structure.ide.layout.PluginWithArtifactPathResult.Companion.logFailures
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.resources.JarsResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import com.jetbrains.plugin.structure.jar.META_INF
import com.jetbrains.plugin.structure.jar.PLUGIN_XML
import com.jetbrains.plugin.structure.jar.PluginDescriptorResult
import com.jetbrains.plugin.structure.jar.PluginJar
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path

private val LOG: Logger = LoggerFactory.getLogger(CorePluginManager::class.java)

private val LIB_DIRECTORY = "lib"

private const val CORE_IDE_PLUGIN_ID = "com.intellij"

/**
 * Manages the core plugin represented `IDE_HOME/lib` directory.
 * These JARs are the content of a special plugin (ID: `com.intellij`)
 *
 * See also [`PluginManagerCore.CORE_PLUGIN_ID`](https://github.com/JetBrains/intellij-community/blob/1fd62dc88af158383087e7fd3860d18b5de798b2/platform/core-impl/src/com/intellij/ide/plugins/PluginManagerCore.kt#L67).
 */
internal class CorePluginManager(private val pluginLoader: LayoutComponentLoader, private val jarFileSystemProvider: JarFileSystemProvider) {
  /**
   * Loads Core plugins from the IDE 'lib' directory.
   *
   * Note that there might be multiple such plugins with multiple descriptors, e. g.
   * `product.jar` with the descriptor in `META-INF/plugin.xml` and `app-client.jar`
   * with the descriptor in `META-INF/PlatformLangPlugin.xml`.
   */
  fun loadCorePlugins(idePath: Path, ideVersion: IdeVersion): List<IdePlugin> {
    val corePluginJarPaths = idePath.resolve(LIB_DIRECTORY).listJars()
    val corePluginJarsResolver = JarsResourceResolver(corePluginJarPaths, jarFileSystemProvider)

    val loadPlugin = { jarPath: Path -> loadPlugin(jarPath, ideVersion, corePluginJarsResolver) }
    val loadedPlugins = corePluginJarPaths.mapNotNull(loadPlugin)
    val loadingResults = LoadingResults(loadedPlugins)
    logFailures(LOG, loadingResults.failures, idePath)
    assertCorePluginsPresent(idePath, loadingResults)

    return loadingResults.successfulPlugins
  }

  private fun assertCorePluginsPresent(
    idePath: Path,
    loadingResults: LoadingResults
  ) {
    if (loadingResults.successfulPlugins.isEmpty()) {
      throw InvalidIdeException(
        idePath, "The 'Core' plugin (ID: 'com.intellij') is expected in the $idePath${File.separator}$LIB_DIRECTORY")
    }
  }

  private fun loadPlugin(jarPath: Path, ideVersion: IdeVersion, platformResourceResolver: ResourceResolver): PluginWithArtifactPathResult? {
    return findDescriptor(jarPath, ideVersion)?.let { descriptor ->
      pluginLoader.load(jarPath, descriptor, platformResourceResolver, ideVersion, CORE_IDE_PLUGIN_ID)
    }
  }

  private fun findDescriptor(jarPath: Path, ideVersion: IdeVersion): String? =
    PluginJar(jarPath, jarFileSystemProvider).use { pluginJar ->
      when (val pluginDescriptorResult = pluginJar.getPluginDescriptor(*ideVersion.descriptorPaths)) {
        is PluginDescriptorResult.Found -> pluginDescriptorResult.path.simpleName
        else -> null
      }
    }

  /**
   * Core plugin filename naming convention is described in the [Knowledge Base](https://youtrack.jetbrains.com/articles/IJPL-A-31).
   */
  private val IdeVersion.descriptorPaths: Array<String>
    get() {
      operator fun String.div(fileName: String) = "$this${File.separator}$fileName"
      return arrayOf(
        META_INF / "${platformPrefix}Plugin.xml",
        META_INF / PLUGIN_XML,
      )
    }

  private val IdeVersion.platformPrefix: String
    get() {
      val platformProduct = IntelliJPlatformProduct.fromIdeVersion(this)
      val product = platformProduct ?: IntelliJPlatformProduct.IDEA
      return product.platformPrefix
    }
}