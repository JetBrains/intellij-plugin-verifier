package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.listFiles
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.ide.layout.LoadingResults
import com.jetbrains.plugin.structure.ide.layout.ModuleFactory
import com.jetbrains.plugin.structure.ide.layout.PlatformPluginManager
import com.jetbrains.plugin.structure.ide.layout.PluginFactory
import com.jetbrains.plugin.structure.ide.layout.PluginWithArtifactPathResult
import com.jetbrains.plugin.structure.ide.layout.PluginWithArtifactPathResult.Companion.logFailures
import com.jetbrains.plugin.structure.ide.layout.PluginWithArtifactPathResult.Failure
import com.jetbrains.plugin.structure.ide.layout.PluginWithArtifactPathResult.Success
import com.jetbrains.plugin.structure.ide.layout.ProductInfoClasspathProvider
import com.jetbrains.plugin.structure.intellij.platform.BundledModulesManager
import com.jetbrains.plugin.structure.intellij.platform.BundledModulesResolver
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
    val platformPlugins = readPlatformPlugins(idePath, ideVersion)
    val plugins = readPlugins(idePath, productInfo, ideVersion)
    return IdeImpl(idePath, ideVersion, platformPlugins + plugins)
  }

  private fun readPlugins(
    idePath: Path,
    productInfo: ProductInfo,
    ideVersion: IdeVersion
  ): List<IdePlugin> {

    val platformResourceResolver = getPlatformResourceResolver(productInfo, idePath)
    val moduleManager = BundledModulesManager(BundledModulesResolver(idePath))

    val productModuleV2Factory = ModuleFactory(::createProductModule, ProductInfoClasspathProvider(productInfo))
    val moduleV2Factory = productModuleV2Factory
    val pluginFactory = PluginFactory(::createPlugin)

    val moduleLoadingResults = productInfo.layout.mapNotNull { layoutComponent ->
      when (layoutComponent) {
        is LayoutComponent.ProductModuleV2 -> {
          productModuleV2Factory.read(layoutComponent, idePath, ideVersion, platformResourceResolver, moduleManager)
        }
        is LayoutComponent.ModuleV2 -> {
          moduleV2Factory.read(layoutComponent, idePath, ideVersion, platformResourceResolver, moduleManager)
        }
        is LayoutComponent.Plugin -> {
          pluginFactory.read(layoutComponent, idePath, ideVersion, platformResourceResolver, moduleManager)
        }
        is LayoutComponent.PluginAlias -> {
          // References to plugin IDs that are already loaded in the other types of layout components
          null
        }
      }
    }.fold(LoadingResults(), LoadingResults::add)

    logFailures(LOG, moduleLoadingResults.failures, idePath)
    return moduleLoadingResults.successfulPlugins
  }

  private fun readPlatformPlugins(idePath: Path, ideVersion: IdeVersion): List<IdePlugin> {
    val platformPluginManager =
      PlatformPluginManager(::createPlugin)
    return platformPluginManager.loadPlatformPlugins(idePath, ideVersion)
  }

  private fun getPlatformResourceResolver(productInfo: ProductInfo, idePath: Path): CompositeResourceResolver {
    val resourceResolvers = productInfo.layout.mapNotNull { it: LayoutComponent ->
      if (it is LayoutComponent.Classpathable) {
        getResourceResolver(it, idePath)
      } else {
        LOG.atDebug().log("No classpath declared for '{}'. Skipping", it)
        null
      }
    }
    return CompositeResourceResolver(resourceResolvers)
  }

  private fun createProductModule(
    pluginArtifactPath: Path,
    descriptorName: String,
    pathResolver: ResourceResolver,
    ideVersion: IdeVersion
  ): PluginWithArtifactPathResult {
    return IdePluginManager
      .createManager(pathResolver)
      .createBundledModule(pluginArtifactPath, ideVersion, descriptorName)
      .withPath(pluginArtifactPath)
  }

  private fun createPlugin(
    pluginArtifactPath: Path,
    descriptorPath: String = PLUGIN_XML,
    resourceResolver: ResourceResolver,
    ideVersion: IdeVersion
  ) = IdePluginManager
    .createManager(resourceResolver)
    .createBundledPlugin(pluginArtifactPath, ideVersion, descriptorPath)
    .withPath(pluginArtifactPath)


  private fun getResourceResolver(layoutComponent: LayoutComponent, idePath: Path): NamedResourceResolver? {
    return if (layoutComponent is LayoutComponent.Classpathable) {
      val itemJarResolvers = layoutComponent.getClasspath().map { jarPath: Path ->
        val fullyQualifiedJarFile = idePath.resolve(jarPath)
        NamedResourceResolver(layoutComponent.name + "#" + jarPath, JarFilesResourceResolver(listOf(fullyQualifiedJarFile)))
      }
      NamedResourceResolver(layoutComponent.name, CompositeResourceResolver(itemJarResolvers))
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

  fun supports(idePath: Path): Boolean = idePath.containsProductInfoJson()



  private fun PluginCreationResult<IdePlugin>.withPath(pluginArtifactPath: Path): PluginWithArtifactPathResult = when (this) {
    is PluginCreationSuccess -> Success(pluginArtifactPath, plugin)
    is PluginCreationFail -> Failure(pluginArtifactPath)
  }


}

