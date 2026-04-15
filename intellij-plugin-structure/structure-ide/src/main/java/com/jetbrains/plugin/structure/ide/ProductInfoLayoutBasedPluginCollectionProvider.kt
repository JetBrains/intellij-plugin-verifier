/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.ide.layout.CorePluginManager
import com.jetbrains.plugin.structure.ide.layout.LayoutComponents
import com.jetbrains.plugin.structure.ide.layout.LayoutComponentsClasspathProvider
import com.jetbrains.plugin.structure.ide.layout.LayoutComponentsNames
import com.jetbrains.plugin.structure.ide.layout.LoadingResults
import com.jetbrains.plugin.structure.ide.layout.ModuleFactory
import com.jetbrains.plugin.structure.ide.layout.PluginFactory
import com.jetbrains.plugin.structure.ide.layout.PluginWithArtifactPathResult
import com.jetbrains.plugin.structure.ide.layout.PluginWithArtifactPathResult.Companion.logFailures
import com.jetbrains.plugin.structure.ide.layout.PluginWithArtifactPathResult.Failure
import com.jetbrains.plugin.structure.ide.layout.PluginWithArtifactPathResult.Success
import com.jetbrains.plugin.structure.ide.resolver.ProductInfoResourceResolver
import com.jetbrains.plugin.structure.intellij.platform.BundledModulesManager
import com.jetbrains.plugin.structure.intellij.platform.BundledModulesResolver
import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.problems.IntelliJPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.JetBrainsPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import com.jetbrains.plugin.structure.jar.PLUGIN_XML
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

private val LOG: Logger = LoggerFactory.getLogger(ProductInfoLayoutBasedPluginCollectionProvider::class.java)

class ProductInfoLayoutBasedPluginCollectionProvider(
  private val additionalPluginReader: ProductInfoBasedIdeManager.PluginReader<LayoutComponents>,
  private val jarFileSystemProvider: JarFileSystemProvider,
) : PluginCollectionProvider<Path> {

  private val loadingContexts = ConcurrentHashMap<ProductInfoLayoutComponentsPluginCollectionSource, LoadingContext>()

  /**
   * Problem level remapping used for bundled plugins.
   */
  private val bundledPluginCreationResultResolver: PluginCreationResultResolver
    get() = JetBrainsPluginCreationResultResolver.fromClassPathJson(IntelliJPluginCreationResultResolver())

  @Throws(IOException::class)
  override fun getPlugins(source: PluginCollectionSource<Path, *>): Collection<IdePlugin> {
    if (source !is ProductInfoLayoutComponentsPluginCollectionSource) {
      return emptySet()
    }
    val context = getLoadingContext(source)
    val corePlugin = context.readCorePlugin()
    val plugins = context.readPlugins()
    val additionalPlugins = source.readAdditionalPlugins()

    return corePlugin + plugins + additionalPlugins
  }

  fun getCorePlugin(source: ProductInfoLayoutComponentsPluginCollectionSource, pluginId: String): IdePlugin? {
    return getLoadingContext(source).readCorePlugin().firstOrNull { it.pluginId == pluginId || it.pluginName == pluginId }
  }

  fun getPlugin(source: ProductInfoLayoutComponentsPluginCollectionSource, layoutComponentName: String): IdePlugin? {
    return getLoadingContext(source).readLayoutComponent(layoutComponentName)
  }

  fun getModule(source: ProductInfoLayoutComponentsPluginCollectionSource, moduleName: String): IdePlugin? {
    return getLoadingContext(source).readModule(moduleName)
  }

  private fun getLoadingContext(source: ProductInfoLayoutComponentsPluginCollectionSource): LoadingContext {
    return loadingContexts.computeIfAbsent(source, ::LoadingContext)
  }

  private fun ProductInfoLayoutComponentsPluginCollectionSource.readAdditionalPlugins(): List<IdePlugin> {
    val layoutComponentNames = LayoutComponentsNames(layoutComponents)
    return additionalPluginReader.readPlugins(idePath, layoutComponents, layoutComponentNames, ideVersion)
  }

  private inner class LoadingContext(
    private val source: ProductInfoLayoutComponentsPluginCollectionSource,
  ) {
    private val layoutComponentsByName = source.layoutComponents.content
      .filterNot { it is LayoutComponent.PluginAlias }
      .associateBy { it.name }

    private val platformResourceResolver by lazy {
      ProductInfoResourceResolver(source.layoutComponents, jarFileSystemProvider)
    }
    private val moduleManager by lazy {
      BundledModulesManager(BundledModulesResolver(source.idePath, jarFileSystemProvider))
    }
    private val idePluginManager by lazy {
      IdePluginManager.createManager(platformResourceResolver)
    }
    private val moduleFactory by lazy {
      ModuleFactory(
        { pluginArtifactPath, descriptorName, resourceResolver, ideVersion, layoutComponentName ->
          createModule(idePluginManager, pluginArtifactPath, descriptorName, resourceResolver, ideVersion, layoutComponentName)
        },
        LayoutComponentsClasspathProvider(source.layoutComponents)
      )
    }
    private val pluginFactory by lazy {
      PluginFactory { pluginArtifactPath, descriptorPath, resourceResolver, ideVersion, layoutComponentName ->
        createPlugin(idePluginManager, pluginArtifactPath, descriptorPath, resourceResolver, ideVersion, layoutComponentName)
      }
    }
    private val loadedLayoutComponents = ConcurrentHashMap<String, PluginWithArtifactPathResult?>()
    private val corePlugins by lazy {
      val corePluginManager = CorePluginManager(
        { pluginArtifactPath, descriptorPath, resourceResolver, ideVersion, layoutComponentName ->
          createPlugin(
            IdePluginManager.createManager(resourceResolver),
            pluginArtifactPath,
            descriptorPath,
            resourceResolver,
            ideVersion,
            layoutComponentName
          )
        },
        jarFileSystemProvider
      )
      corePluginManager.loadCorePlugins(source.idePath, source.ideVersion)
    }
    private val plugins by lazy {
      val loadingResults = source.layoutComponents.content.mapNotNull { layoutComponent ->
        readLayoutComponentResult(layoutComponent.name)
      }.fold(LoadingResults(), LoadingResults::add)

      logFailures(LOG, loadingResults.failures, source.idePath)
      loadingResults.successfulPlugins
    }

    fun readCorePlugin(): List<IdePlugin> = corePlugins

    fun readPlugins(): List<IdePlugin> = plugins

    fun readModule(moduleName: String): IdePlugin? {
      val layoutComponent = layoutComponentsByName[moduleName]
      if (layoutComponent !is LayoutComponent.ModuleV2 && layoutComponent !is LayoutComponent.ProductModuleV2) {
        return null
      }
      return readLayoutComponent(moduleName)
    }

    fun readLayoutComponent(layoutComponentName: String): IdePlugin? {
      return when (val result = readLayoutComponentResult(layoutComponentName)) {
        is Success -> result.plugin
        else -> null
      }
    }

    private fun readLayoutComponentResult(layoutComponentName: String): PluginWithArtifactPathResult? {
      return loadedLayoutComponents.computeIfAbsent(layoutComponentName) {
        readLayoutComponentResult(layoutComponentsByName[it])
      }
    }

    private fun readLayoutComponentResult(layoutComponent: LayoutComponent?): PluginWithArtifactPathResult? {
      if (layoutComponent == null) {
        return null
      }
      return when (layoutComponent) {
        is LayoutComponent.ModuleV2,
        is LayoutComponent.ProductModuleV2 -> {
          moduleFactory.read(layoutComponent, source.idePath, source.ideVersion, platformResourceResolver, moduleManager)
        }

        is LayoutComponent.Plugin -> {
          pluginFactory.read(layoutComponent, source.idePath, source.ideVersion, platformResourceResolver, moduleManager)
        }

        is LayoutComponent.PluginAlias -> null
      }
    }
  }

  private fun createModule(
    idePluginManager: IdePluginManager,
    pluginArtifactPath: Path,
    descriptorName: String,
    @Suppress("UNUSED_PARAMETER") _resourceResolver: ResourceResolver,
    ideVersion: IdeVersion,
    @Suppress("UNUSED_PARAMETER") _layoutComponentName: String
  ): PluginWithArtifactPathResult {
    return idePluginManager
      .createBundledModule(pluginArtifactPath, ideVersion, descriptorName, bundledPluginCreationResultResolver)
      .withPath(pluginArtifactPath)
  }

  private fun createPlugin(
    idePluginManager: IdePluginManager,
    pluginArtifactPath: Path,
    descriptorPath: String = PLUGIN_XML,
    @Suppress("UNUSED_PARAMETER") _resourceResolver: ResourceResolver,
    ideVersion: IdeVersion,
    layoutComponentName: String
  ) = idePluginManager
    .createBundledPlugin(pluginArtifactPath, ideVersion, descriptorPath, layoutComponentName)
    .withPath(pluginArtifactPath)

  private fun IdePluginManager.createBundledPlugin(
    pluginArtifact: Path,
    ideVersion: IdeVersion,
    descriptorPath: String,
    layoutComponentName: String
  ): PluginCreationResult<IdePlugin> {
    return createBundledPlugin(
      resolvePluginArtifact(pluginArtifact),
      ideVersion,
      descriptorPath,
      bundledPluginCreationResultResolver,
      fallbackPluginId = layoutComponentName
    )
  }

  private fun resolvePluginArtifact(path: Path): Path {
    return if (path.isJar() && path.parent.fileName.toString() == LIB_DIRECTORY) {
      path.parent.parent
    } else {
      path
    }
  }

  private fun PluginCreationResult<IdePlugin>.withPath(pluginArtifactPath: Path): PluginWithArtifactPathResult = when (this) {
    is PluginCreationSuccess -> Success(pluginArtifactPath, plugin)
    is PluginCreationFail -> Failure(pluginArtifactPath, errorsAndWarnings)
  }
}
