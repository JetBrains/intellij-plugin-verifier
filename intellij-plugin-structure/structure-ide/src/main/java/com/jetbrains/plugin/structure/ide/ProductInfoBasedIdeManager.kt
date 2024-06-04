package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.listFiles
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.platform.ProductInfoParseException
import com.jetbrains.plugin.structure.intellij.platform.ProductInfoParser
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.plugin.JarFilesResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.CompositeResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.NamedResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.jar.PLUGIN_XML
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Path

private const val PRODUCT_INFO_JSON = "product-info.json"
private val VERSION_FROM_PRODUCT_INFO: IdeVersion? = null

private val LOG: Logger = LoggerFactory.getLogger(ProductInfoBasedIdeManager::class.java)

class ProductInfoBasedIdeManager : IdeManager() {
  private val productInfoParser = ProductInfoParser()

  @Throws(InvalidIdeException::class)
  override fun createIde(idePath: Path): Ide = createIde(idePath, VERSION_FROM_PRODUCT_INFO)

  override fun createIde(idePath: Path, version: IdeVersion?): Ide {
    assertProductInfoPresent(idePath)
    try {
      val productInfo = productInfoParser.parse(idePath.productInfoJson)
      val ideVersion = version ?: IdeVersion.createIdeVersion(productInfo.buildNumber)
      return createIde(idePath, ideVersion, productInfo)
    } catch (e: ProductInfoParseException) {
      throw InvalidIdeException(idePath, e)
    }
  }

  private fun createIde(idePath: Path, ideVersion: IdeVersion, productInfo: ProductInfo): Ide {
    if (!idePath.isDirectory) {
      throw IOException("Specified path does not exist or is not a directory: $idePath")
    }
    val plugins = readPlugins(idePath, productInfo, ideVersion)
    return IdeImpl(idePath, ideVersion, plugins)
  }

  private fun readPlugins(
    idePath: Path,
    productInfo: ProductInfo,
    ideVersion: IdeVersion
  ): List<IdePlugin> {

    val platformResourceResolver = getPlatformResourceResolver(productInfo)
    val relativePluginArtifactPaths = productInfo.layout.mapNotNull {
      if (it is LayoutComponent.Classpathable) {
        getCommonParentDirectory(it.getClasspath())?.let { commonParent ->
          if (commonParent.simpleName == "lib") {
            commonParent.parent
          } else {
            commonParent
          }
        }
      } else {
        null
      }
    }
    val pluginArtifactPaths = relativePluginArtifactPaths.map { idePath.resolve(it) }

    val (successes, failures) = pluginArtifactPaths.map {
      createPlugin(it, platformResourceResolver, ideVersion)
    }.partition {
      it.result is PluginCreationSuccess
    }
    logFailures(failures, idePath)
    return successes.map {
      (it.result as PluginCreationSuccess).plugin
    }
  }

  private fun getPlatformResourceResolver(productInfo: ProductInfo): CompositeResourceResolver {
    val resourceResolvers = productInfo.layout.mapNotNull { it: LayoutComponent ->
      if (it is LayoutComponent.Classpathable) {
        it.resourceResolver()
      } else {
        LOG.atDebug().log("No classpath declared for '{}'. Skipping", it)
        null
      }
    }
    val platformResourceResolver = CompositeResourceResolver(resourceResolvers)
    return platformResourceResolver
  }

  private fun createPlugin(
    pluginArtifactPath: Path,
    pathResolver: ResourceResolver,
    ideVersion: IdeVersion
  ) = PluginPathAndResult(pluginArtifactPath, IdePluginManager
    .createManager(pathResolver)
    .createBundledPlugin(pluginArtifactPath, ideVersion, PLUGIN_XML))

  private fun logFailures(
    failures: List<PluginPathAndResult>,
    idePath: Path
  ) {
    if (failures.isNotEmpty()) {
      val failedPluginPaths = failures.map {
        idePath.relativize(it.pluginArtifactPath)
      }.joinToString(", ")
      LOG.atWarn().log("Following plugins (${failures.size}) could not be created: $failedPluginPaths")
    }
  }



  private fun LayoutComponent.resourceResolver(): NamedResourceResolver? {
    return if (this is LayoutComponent.Classpathable) {
      val itemJarResolvers = getClasspath().map {
        NamedResourceResolver(this.name + "#" + it, JarFilesResourceResolver(listOf(it)))
      }
      NamedResourceResolver(name, CompositeResourceResolver(itemJarResolvers))
    } else {
      null
    }
  }

  private fun Path.containsProductInfoJson(): Boolean =
    listFiles().any { it.simpleName == PRODUCT_INFO_JSON }

  private val Path.productInfoJson: Path
    get() {
      return resolve(PRODUCT_INFO_JSON)
    }

  @Throws(InvalidIdeException::class)
  private fun assertProductInfoPresent(idePath: Path) {
    if (!idePath.containsProductInfoJson()) {
      throw InvalidIdeException(idePath, "The '$PRODUCT_INFO_JSON' file is not available. This file should be present for IDE versions 2024.2 and newer")
    }
  }

  private data class PluginPathAndResult(val pluginArtifactPath: Path, val result: PluginCreationResult<IdePlugin>)
}