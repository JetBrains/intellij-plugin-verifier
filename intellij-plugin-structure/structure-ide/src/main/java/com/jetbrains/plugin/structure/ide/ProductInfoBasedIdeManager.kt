package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.ide.layout.CorePluginManager
import com.jetbrains.plugin.structure.ide.layout.LoadingResults
import com.jetbrains.plugin.structure.ide.layout.ModuleFactory
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
import com.jetbrains.plugin.structure.intellij.problems.IntelliJPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.JetBrainsPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
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

  private val excludeMissingProductInfoLayoutComponents = true

  /**
   * Problem level remapping used for bundled plugins.
   */
  private val bundledPluginCreationResultResolver: PluginCreationResultResolver
    get() = JetBrainsPluginCreationResultResolver.fromClassPathJson(IntelliJPluginCreationResultResolver())

  @Throws(InvalidIdeException::class)
  override fun createIde(idePath: Path): Ide = createIde(idePath, VERSION_FROM_PRODUCT_INFO)

  override fun createIde(idePath: Path, version: IdeVersion?): Ide {
    assertProductInfoPresent(idePath)
    try {
      val productInfo = productInfoParser.parse(idePath.productInfoJson)
      val ideVersion = version ?: createIdeVersion(productInfo)
      return createIde(idePath, ideVersion, productInfo)
    } catch (e: ProductInfoParseException) {
      throw InvalidIdeException(idePath, e)
    }
  }

  private fun createIde(idePath: Path, ideVersion: IdeVersion, productInfo: ProductInfo): Ide {
    if (!idePath.isDirectory) {
      throw IOException("Specified path does not exist or is not a directory: $idePath")
    }
    val corePlugin = readCorePlugin(idePath, ideVersion)
    val plugins = readPlugins(idePath, productInfo, ideVersion)
    return IdeImpl(idePath, ideVersion, corePlugin + plugins)
  }

  private fun readPlugins(
    idePath: Path,
    productInfo: ProductInfo,
    ideVersion: IdeVersion
  ): List<IdePlugin> {

    val layoutComponents = getLayoutComponents(idePath, productInfo)
    val platformResourceResolver = getPlatformResourceResolver(layoutComponents)

    val moduleManager = BundledModulesManager(BundledModulesResolver(idePath))

    val moduleV2Factory = ModuleFactory(::createModule, ProductInfoClasspathProvider(productInfo))
    val pluginFactory = PluginFactory(::createPlugin)

    val moduleLoadingResults = layoutComponents
      .map { it.layoutComponent }
      .mapNotNull { layoutComponent ->
        when (layoutComponent) {
          is LayoutComponent.ModuleV2,
          is LayoutComponent.ProductModuleV2 -> {
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

  private fun readCorePlugin(idePath: Path, ideVersion: IdeVersion): List<IdePlugin> {
    val corePluginManager =
      CorePluginManager(::createPlugin)
    return corePluginManager.loadCorePlugins(idePath, ideVersion)
  }

  private fun getLayoutComponents(idePath: Path, productInfo: ProductInfo): LayoutComponents {
    val layoutComponents = LayoutComponents.of(idePath, productInfo)
    return if (excludeMissingProductInfoLayoutComponents) {
      val (okComponents, failedComponents) = layoutComponents.partition { it.allClasspathsExist() }
      logUnavailableClasspath(failedComponents)
      LayoutComponents(okComponents)
    } else {
      layoutComponents
    }
  }

  private fun getPlatformResourceResolver(layoutComponents: LayoutComponents): CompositeResourceResolver {
    val resourceResolvers = layoutComponents.mapNotNull {
      if (it.isClasspathable) {
        getResourceResolver(it)
      } else {
        LOG.atDebug().log("No classpath declared for '{}'. Skipping", it.layoutComponent)
        null
      }
    }
    return CompositeResourceResolver(resourceResolvers)
  }

  private fun createModule(
    pluginArtifactPath: Path,
    descriptorName: String,
    pathResolver: ResourceResolver,
    ideVersion: IdeVersion
  ): PluginWithArtifactPathResult {
    return IdePluginManager
      .createManager(pathResolver)
      .createBundledModule(pluginArtifactPath, ideVersion, descriptorName, bundledPluginCreationResultResolver)
      .withPath(pluginArtifactPath)
  }

  private fun createPlugin(
    pluginArtifactPath: Path,
    descriptorPath: String = PLUGIN_XML,
    resourceResolver: ResourceResolver,
    ideVersion: IdeVersion
  ) = IdePluginManager
    .createManager(resourceResolver)
    .createBundledPlugin(pluginArtifactPath, ideVersion, descriptorPath, bundledPluginCreationResultResolver)
    .withPath(pluginArtifactPath)

  private fun createIdeVersion(productInfo: ProductInfo): IdeVersion {
    val versionString = buildString {
      with(productInfo) {
        if (productCode.isNotEmpty()) append(productCode).append("-")
        append(buildNumber)
      }
    }
    return IdeVersion.createIdeVersion(versionString)
  }

  private fun getResourceResolver(layoutComponent: ResolvedLayoutComponent): NamedResourceResolver? {
    if (!layoutComponent.isClasspathable) {
      return null
    }
    val itemJarResolvers = layoutComponent.resolveClasspaths().map {
      NamedResourceResolver(
        layoutComponent.name + "#" + it.relativePath, JarFilesResourceResolver(it.toList())
      )
    }
    return NamedResourceResolver(layoutComponent.name, CompositeResourceResolver(itemJarResolvers))
  }

  private fun logUnavailableClasspath(failedComponents: List<ResolvedLayoutComponent>) {
    val logMsg = failedComponents.joinToString("\n") {
      val cp = it.getClasspaths().joinToString(", ")
      "Layout component '${it.name}' has some nonexistent 'classPath' elements: '$cp'"
    }
    LOG.atDebug().log(logMsg)
  }

  private fun Path.containsProductInfoJson(): Boolean = resolve(PRODUCT_INFO_JSON).exists()

  private val Path.productInfoJson: Path
    get() {
      return resolve(PRODUCT_INFO_JSON)
    }

  @Throws(InvalidIdeException::class)
  private fun assertProductInfoPresent(idePath: Path) {
    if (!idePath.containsProductInfoJson()) {
      throw InvalidIdeException(idePath, "The '$PRODUCT_INFO_JSON' file is not available.")
    }
  }

  fun supports(idePath: Path): Boolean = idePath.containsProductInfoJson()
    && isAtLeastVersion(idePath, "242")

  private fun isAtLeastVersion(idePath: Path, expectedVersion: String): Boolean {
    return when (val version = BuildTxtIdeVersionProvider().readIdeVersion(idePath)) {
      is IdeVersionResolution.Found -> version.ideVersion > IdeVersion.createIdeVersion(expectedVersion)
      is IdeVersionResolution.Failed,
      is IdeVersionResolution.NotFound -> false
    }
  }

  private fun PluginCreationResult<IdePlugin>.withPath(pluginArtifactPath: Path): PluginWithArtifactPathResult = when (this) {
    is PluginCreationSuccess -> Success(pluginArtifactPath, plugin)
    is PluginCreationFail -> Failure(pluginArtifactPath, errorsAndWarnings)
  }
}

private class LayoutComponents(val layoutComponents: List<ResolvedLayoutComponent>) : Iterable<ResolvedLayoutComponent> {
  companion object {
    fun of(idePath: Path, productInfo: ProductInfo): LayoutComponents {
      val resolvedLayoutComponents = productInfo.layout
        .map { ResolvedLayoutComponent(idePath, it) }
      return LayoutComponents(resolvedLayoutComponents)
    }
  }

  override fun iterator() = layoutComponents.iterator()
}

private data class IdeRelativePath(val idePath: Path, val relativePath: Path) {
  val resolvedPath: Path? = idePath.resolve(relativePath)

  val exists: Boolean
    get() = resolvedPath?.exists() ?: false

  fun toList(): List<Path> = if (resolvedPath != null) listOf(resolvedPath) else emptyList()
}

private data class ResolvedLayoutComponent(val idePath: Path, val layoutComponent: LayoutComponent) {
  val name: String
    get() = layoutComponent.name

  fun getClasspaths(): List<Path> {
    return if (layoutComponent is LayoutComponent.Classpathable) {
      layoutComponent.getClasspath()
    } else {
      emptyList()
    }
  }

  fun resolveClasspaths(): List<IdeRelativePath> {
    return if (layoutComponent is LayoutComponent.Classpathable) {
      layoutComponent.getClasspath().map { IdeRelativePath(idePath, it) }
    } else {
      emptyList()
    }
  }

  fun allClasspathsExist(): Boolean {
    return resolveClasspaths().all { it.exists }
  }

  val isClasspathable: Boolean
    get() = layoutComponent is LayoutComponent.Classpathable
}
