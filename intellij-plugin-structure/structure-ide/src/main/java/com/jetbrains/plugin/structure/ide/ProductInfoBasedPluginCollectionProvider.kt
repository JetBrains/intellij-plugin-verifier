/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.ide.layout.CorePluginManager
import com.jetbrains.plugin.structure.ide.layout.LoadingResults
import com.jetbrains.plugin.structure.ide.layout.MissingLayoutFileMode
import com.jetbrains.plugin.structure.ide.layout.MissingLayoutFileMode.SKIP_AND_WARN
import com.jetbrains.plugin.structure.ide.layout.ModuleFactory
import com.jetbrains.plugin.structure.ide.layout.PluginFactory
import com.jetbrains.plugin.structure.ide.layout.PluginWithArtifactPathResult
import com.jetbrains.plugin.structure.ide.layout.PluginWithArtifactPathResult.Companion.logFailures
import com.jetbrains.plugin.structure.ide.layout.PluginWithArtifactPathResult.Failure
import com.jetbrains.plugin.structure.ide.layout.PluginWithArtifactPathResult.Success
import com.jetbrains.plugin.structure.ide.layout.ProductInfoClasspathProvider
import com.jetbrains.plugin.structure.ide.resolver.LayoutComponentsProvider
import com.jetbrains.plugin.structure.ide.resolver.ProductInfoResourceResolver
import com.jetbrains.plugin.structure.intellij.platform.BundledModulesManager
import com.jetbrains.plugin.structure.intellij.platform.BundledModulesResolver
import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
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

private val LOG: Logger = LoggerFactory.getLogger(ProductInfoBasedPluginCollectionProvider::class.java)

class ProductInfoBasedPluginCollectionProvider(
  missingLayoutFileMode: MissingLayoutFileMode = SKIP_AND_WARN,
  private val additionalPluginReader: ProductInfoBasedIdeManager.PluginReader,
  private val jarFileSystemProvider: JarFileSystemProvider,
) : PluginCollectionProvider<Path> {

  private val layoutComponentProvider = LayoutComponentsProvider(missingLayoutFileMode)

  /**
   * Problem level remapping used for bundled plugins.
   */
  private val bundledPluginCreationResultResolver: PluginCreationResultResolver
    get() = JetBrainsPluginCreationResultResolver.fromClassPathJson(IntelliJPluginCreationResultResolver())

  @Throws(IOException::class)
  override fun getPlugins(source: PluginCollectionSource<Path>): Collection<IdePlugin> {
    if (source !is ProductInfoPluginCollectionSource) {
      return emptySet()
    }
    val (idePath, ideVersion, productInfo) = source
    val corePlugin = readCorePlugin(idePath, ideVersion)
    val plugins = readPlugins(idePath, productInfo, ideVersion)
    val additionalPlugins = readAdditionalPlugins(idePath, productInfo, ideVersion)

    return corePlugin + plugins + additionalPlugins
  }

  private fun readPlugins(
    idePath: Path,
    productInfo: ProductInfo,
    ideVersion: IdeVersion
  ): List<IdePlugin> {
    val layoutComponents = layoutComponentProvider.resolveLayoutComponents(productInfo, idePath)

    val platformResourceResolver = ProductInfoResourceResolver(productInfo, idePath, layoutComponentProvider, jarFileSystemProvider)
    val moduleManager = BundledModulesManager(BundledModulesResolver(idePath, jarFileSystemProvider))

    val moduleV2Factory = ModuleFactory(::createModule, ProductInfoClasspathProvider(productInfo))
    val pluginFactory = PluginFactory(::createPlugin)

    val moduleLoadingResults = layoutComponents.content.mapNotNull { layoutComponent ->
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
      CorePluginManager(::createPlugin, jarFileSystemProvider)
    return corePluginManager.loadCorePlugins(idePath, ideVersion)
  }

  private fun readAdditionalPlugins(
    idePath: Path,
    productInfo: ProductInfo,
    ideVersion: IdeVersion
  ): List<IdePlugin> {
    return additionalPluginReader.readPlugins(idePath, productInfo, ideVersion)
  }

  private fun createModule(
    pluginArtifactPath: Path,
    descriptorName: String,
    pathResolver: ResourceResolver,
    ideVersion: IdeVersion,
    @Suppress("unused") layoutComponentName: String
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
    ideVersion: IdeVersion,
    layoutComponentName: String
  ) = IdePluginManager
    .createManager(resourceResolver)
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