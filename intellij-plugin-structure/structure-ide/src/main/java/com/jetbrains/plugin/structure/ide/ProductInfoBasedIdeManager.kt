package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.listFiles
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.ide.ProductInfoBasedIdeManager.PluginWithArtifactPathResult.Failure
import com.jetbrains.plugin.structure.ide.ProductInfoBasedIdeManager.PluginWithArtifactPathResult.Success
import com.jetbrains.plugin.structure.ide.layout.ProductModuleV2Factory
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
    val plugins = readPlugins(idePath, productInfo, ideVersion)
    return IdeImpl(idePath, ideVersion, plugins)
  }

  private fun readPlugins(
    idePath: Path,
    productInfo: ProductInfo,
    ideVersion: IdeVersion
  ): List<IdePlugin> {

    val platformResourceResolver = getPlatformResourceResolver(productInfo, idePath)
    val moduleManager = BundledModulesManager(BundledModulesResolver(idePath))

    val moduleLoadingResults = LoadingResults()

    val productModuleV2Factory = ProductModuleV2Factory(this::createProductModule)
    productInfo.layout.filterIsInstance<LayoutComponent.ProductModuleV2>()
      .mapNotNull { productModuleV2Factory.read(it, idePath, ideVersion, platformResourceResolver, moduleManager) }
      .let {
        moduleLoadingResults.add(it)
      }

    val relativePluginArtifactPaths = productInfo.layout.mapNotNull {
      if (it is LayoutComponent.ProductModuleV2) {
        LOG.atDebug().log("Skipping {}", it)
        null
      } else if (it is LayoutComponent.Classpathable) {
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

    pluginArtifactPaths.map {
      createPlugin(it, platformResourceResolver, ideVersion)
    }.let {
      moduleLoadingResults.add(it)
    }

    logFailures(moduleLoadingResults.failures, idePath)
    return moduleLoadingResults.successfulPlugins
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
    val platformResourceResolver = CompositeResourceResolver(resourceResolvers)
    return platformResourceResolver
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
    pathResolver: ResourceResolver,
    ideVersion: IdeVersion
  ) = IdePluginManager
    .createManager(pathResolver)
    .createBundledPlugin(pluginArtifactPath, ideVersion, PLUGIN_XML)
    .withPath(pluginArtifactPath)

  private fun logFailures(
    failures: List<PluginWithArtifactPathResult>,
    idePath: Path
  ) {
    if (failures.isNotEmpty()) {
      val failedPluginPaths = failures.map {
        idePath.relativize(it.pluginArtifactPath)
      }.joinToString(", ")
      LOG.atWarn().log("Following ${failures.size} plugins could not be created: $failedPluginPaths")
    }
  }

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

  sealed class PluginWithArtifactPathResult(open val pluginArtifactPath: Path) {
    data class Success(override val pluginArtifactPath: Path, val plugin: IdePlugin) : PluginWithArtifactPathResult(pluginArtifactPath)
    data class Failure(override val pluginArtifactPath: Path) : PluginWithArtifactPathResult(pluginArtifactPath)
  }

  private fun PluginCreationResult<IdePlugin>.withPath(pluginArtifactPath: Path): PluginWithArtifactPathResult = when (this) {
    is PluginCreationSuccess -> Success(pluginArtifactPath, plugin)
    is PluginCreationFail -> Failure(pluginArtifactPath)
  }

  private class LoadingResults {
    private val _successes = mutableListOf<Success>()
    private val _failures = mutableListOf<Failure>()

    val successes: List<Success>
      get() = _successes

    val failures: List<Failure>
      get() = _failures

    val successfulPlugins: List<IdePlugin>
      get() = successes.map { it.plugin }

    fun add(results: List<PluginWithArtifactPathResult>) {
      val (successes, failures) = results.partition {
        it is Success
      }
      _successes += successes.map { it as Success }
      _failures += failures.map { it as Failure }
    }
  }
}

