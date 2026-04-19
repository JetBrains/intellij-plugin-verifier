/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.MissedFile
import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.ide.layout.CorePluginManager
import com.jetbrains.plugin.structure.ide.layout.LayoutComponents
import com.jetbrains.plugin.structure.ide.layout.LayoutComponentsClasspathProvider
import com.jetbrains.plugin.structure.ide.layout.LayoutComponentsNames
import com.jetbrains.plugin.structure.ide.layout.LayoutComponentLoader
import com.jetbrains.plugin.structure.ide.layout.ModuleFactory
import com.jetbrains.plugin.structure.ide.layout.PluginWithArtifactPathResult
import com.jetbrains.plugin.structure.ide.layout.PluginWithArtifactPathResult.Companion.logFailures
import com.jetbrains.plugin.structure.ide.layout.ResolvedLayoutComponent
import com.jetbrains.plugin.structure.ide.resolver.ProductInfoResourceResolver
import com.jetbrains.plugin.structure.intellij.platform.BundledModulesManager
import com.jetbrains.plugin.structure.intellij.platform.BundledModulesResolver
import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent
import com.jetbrains.plugin.structure.intellij.plugin.Classpath
import com.jetbrains.plugin.structure.intellij.plugin.ClasspathOrigin
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.plugin.LibDirJarsClasspathProvider
import com.jetbrains.plugin.structure.intellij.plugin.PluginFileNotFoundException
import com.jetbrains.plugin.structure.intellij.plugin.module.ContentModuleScanner
import com.jetbrains.plugin.structure.intellij.problems.IntelliJPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.JetBrainsPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.CompositeResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.JarsResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import com.jetbrains.plugin.structure.jar.PLUGIN_XML
import com.jetbrains.plugin.structure.jar.PluginJar
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

private val LOG: Logger = LoggerFactory.getLogger(ProductInfoLayoutBasedPluginCollectionProvider::class.java)

private const val CORE_IDE_PLUGIN_ID = "com.intellij"

class ProductInfoLayoutBasedPluginCollectionProvider(
  private val additionalPluginReader: ProductInfoBasedIdeManager.PluginReader<LayoutComponents>,
  private val jarFileSystemProvider: JarFileSystemProvider,
) : TargetedPluginCollectionProvider<Path> {
  private val libDirJarsClasspathProvider = LibDirJarsClasspathProvider()
  private val contentModuleScanner = ContentModuleScanner(jarFileSystemProvider)

  /**
   * Problem level remapping used for bundled plugins.
   */
  private val bundledPluginCreationResultResolver: PluginCreationResultResolver
    get() = JetBrainsPluginCreationResultResolver.fromClassPathJson(IntelliJPluginCreationResultResolver())

  private val stateBySource = ConcurrentHashMap<ProductInfoLayoutComponentsPluginCollectionSource, State>()

  @Throws(IOException::class)
  override fun getPlugins(source: PluginCollectionSource<Path, *>): Collection<IdePlugin> {
    return if (source is ProductInfoLayoutComponentsPluginCollectionSource) {
      state(source).getPlugins()
    } else {
      emptySet()
    }
  }

  override fun findPluginById(source: PluginCollectionSource<Path, *>, pluginId: String): PluginLookupResult {
    return if (source is ProductInfoLayoutComponentsPluginCollectionSource) {
      state(source).findPluginById(pluginId)
    } else {
      PluginLookupResult.unsupported()
    }
  }

  override fun findPluginByModule(source: PluginCollectionSource<Path, *>, moduleId: String): PluginLookupResult {
    return if (source is ProductInfoLayoutComponentsPluginCollectionSource) {
      state(source).findPluginByModule(moduleId)
    } else {
      PluginLookupResult.unsupported()
    }
  }

  private fun state(source: ProductInfoLayoutComponentsPluginCollectionSource): State {
    return stateBySource.computeIfAbsent(source) { State(it) }
  }

  private inner class State(
    private val source: ProductInfoLayoutComponentsPluginCollectionSource,
  ) {
    private val platformResourceResolver by lazy {
      ProductInfoResourceResolver(source.layoutComponents, jarFileSystemProvider)
    }
    private val moduleManager by lazy {
      BundledModulesManager(BundledModulesResolver(source.idePath, jarFileSystemProvider))
    }
    private val moduleFactory by lazy {
      ModuleFactory(::createModule, LayoutComponentsClasspathProvider(source.layoutComponents))
    }
    private val corePluginManager by lazy {
      CorePluginManager(LayoutComponentLoader(::createPlugin), jarFileSystemProvider)
    }

    private val loadedLayoutComponents = linkedMapOf<ResolvedLayoutComponent, PluginWithArtifactPathResult?>()
    private val pluginsById = linkedMapOf<String, IdePlugin>()
    private val pluginsByModuleId = linkedMapOf<String, IdePlugin>()
    private val layoutComponentsByName = source.layoutComponents.layoutComponents.associateBy { it.name }
    private val directModuleComponentsById = source.layoutComponents.layoutComponents.mapNotNull { resolvedLayoutComponent ->
      when (resolvedLayoutComponent.layoutComponent) {
        is LayoutComponent.ModuleV2,
        is LayoutComponent.ProductModuleV2 -> resolvedLayoutComponent.name to resolvedLayoutComponent
        else -> null
      }
    }.toMap()
    private val notFoundPluginIds = hashSetOf<String>()
    private val notFoundModuleIds = hashSetOf<String>()

    private var corePlugins: List<IdePlugin>? = null
    private var additionalPlugins: List<IdePlugin>? = null

    @Synchronized
    fun getPlugins(): List<IdePlugin> {
      val core = loadCorePlugins()
      val layoutPlugins = source.layoutComponents.layoutComponents.mapNotNull { loadLayoutComponent(it) }
      val failures = loadedLayoutComponents.values.filterIsInstance<PluginWithArtifactPathResult.Failure>()
      logFailures(LOG, failures, source.idePath)
      return core + layoutPlugins + loadAdditionalPlugins()
    }

    @Synchronized
    fun findPluginById(pluginId: String): PluginLookupResult {
      pluginsById[pluginId]?.let { return PluginLookupResult.found(it) }
      if (pluginId in notFoundPluginIds) {
        return PluginLookupResult.notFound()
      }

      if (pluginId == CORE_IDE_PLUGIN_ID) {
        loadCorePlugins()
        pluginsById[pluginId]?.let { return PluginLookupResult.found(it) }
      }

      findLayoutComponentByName(pluginId)?.let { component ->
        loadLayoutComponent(component)
        pluginsById[pluginId]?.let { return PluginLookupResult.found(it) }
      }

      notFoundPluginIds += pluginId
      return PluginLookupResult.notFound()
    }

    @Synchronized
    fun findPluginByModule(moduleId: String): PluginLookupResult {
      pluginsByModuleId[moduleId]?.let { return PluginLookupResult.found(it) }
      if (moduleId in notFoundModuleIds) {
        return PluginLookupResult.notFound()
      }

      loadCorePlugins()
      pluginsByModuleId[moduleId]?.let { return PluginLookupResult.found(it) }

      findDirectModuleComponent(moduleId)?.let { component ->
        loadLayoutComponent(component)
        pluginsByModuleId[moduleId]?.let { return PluginLookupResult.found(it) }
      }

      source.layoutComponents.layoutComponents.forEach { component ->
        loadLayoutComponent(component)
        pluginsByModuleId[moduleId]?.let { return PluginLookupResult.found(it) }
      }

      notFoundModuleIds += moduleId
      return PluginLookupResult.notFound()
    }

    private fun findLayoutComponentByName(name: String): ResolvedLayoutComponent? {
      return layoutComponentsByName[name]
    }

    private fun findDirectModuleComponent(moduleId: String): ResolvedLayoutComponent? {
      return directModuleComponentsById[moduleId]
    }

    private fun loadCorePlugins(): List<IdePlugin> {
      return corePlugins ?: corePluginManager.loadCorePlugins(source.idePath, source.ideVersion).also { plugins ->
        corePlugins = plugins
        plugins.forEach(::indexPlugin)
      }
    }

    private fun loadAdditionalPlugins(): List<IdePlugin> {
      return additionalPlugins ?: run {
        val layoutComponentNames = LayoutComponentsNames(source.layoutComponents)
        additionalPluginReader.readPlugins(
          source.idePath,
          source.layoutComponents,
          layoutComponentNames,
          source.ideVersion,
        ).also { plugins ->
          additionalPlugins = plugins
          plugins.forEach(::indexPlugin)
        }
      }
    }

    private fun loadLayoutComponent(layoutComponent: ResolvedLayoutComponent): IdePlugin? {
      if (layoutComponent.layoutComponent is LayoutComponent.PluginAlias || layoutComponent.name == CORE_IDE_PLUGIN_ID) {
        return null
      }

      val result = loadedLayoutComponents.getOrPut(layoutComponent) {
        when (val component = layoutComponent.layoutComponent) {
          is LayoutComponent.ModuleV2,
          is LayoutComponent.ProductModuleV2 -> moduleFactory.read(
            component,
            source.idePath,
            source.ideVersion,
            platformResourceResolver,
            moduleManager,
          )

          is LayoutComponent.Plugin -> loadPluginComponent(component)
          is LayoutComponent.PluginAlias -> null
        }
      }

      val success = result as? PluginWithArtifactPathResult.Success ?: return null
      indexPlugin(success.plugin)
      return success.plugin
    }

    private fun loadPluginComponent(layoutComponent: LayoutComponent.Plugin): PluginWithArtifactPathResult? {
      val relativeClasspath = layoutComponent.getClasspath()
      if (relativeClasspath.isEmpty()) {
        return null
      }
      val classpath = relativeClasspath.map(source.idePath::resolve)
      val pluginArtifactPath = source.idePath.resolve(resolvePluginArtifact(relativeClasspath)).normalize()
      val existingJars = classpath.filter(Path::isJar)
      val pluginResourceResolver = pluginResourceResolver(existingJars)

      val pluginLoadPath = findDescriptorHolder(existingJars) ?: pluginArtifactPath
      val resolvedClasspath = resolvePluginClasspath(pluginArtifactPath, existingJars)

      return createPlugin(
        pluginLoadPath = pluginLoadPath,
        pluginArtifactPath = pluginArtifactPath,
        descriptorPath = PLUGIN_XML,
        resourceResolver = pluginResourceResolver,
        ideVersion = source.ideVersion,
        layoutComponentName = layoutComponent.name,
        classpath = resolvedClasspath,
      )
    }

    private fun pluginResourceResolver(pluginClasspaths: List<Path>): ResourceResolver {
      val pluginJarsResolver = if (pluginClasspaths.isEmpty()) {
        null
      } else {
        JarsResourceResolver(pluginClasspaths, jarFileSystemProvider)
      }

      return when (pluginJarsResolver) {
        null -> platformResourceResolver
        else -> CompositeResourceResolver(listOf(pluginJarsResolver, platformResourceResolver))
      }
    }

    private fun findDescriptorHolder(classpath: List<Path>): Path? {
      return classpath.firstOrNull { path ->
        runCatching {
          PluginJar(path, jarFileSystemProvider).use { pluginJar ->
            pluginJar.resolveDescriptorPath() != null
          }
        }.getOrDefault(false)
      }
    }
    private fun indexPlugin(plugin: IdePlugin) {
      val pluginId = plugin.pluginId ?: plugin.pluginName
      if (pluginId != null) {
        pluginsById.putIfAbsent(pluginId, plugin)
        notFoundPluginIds.remove(pluginId)
      }
      plugin.definedModules.forEach { moduleId ->
        pluginsByModuleId.putIfAbsent(moduleId, plugin)
        notFoundModuleIds.remove(moduleId)
      }
    }
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
  ): PluginWithArtifactPathResult {
    val resolvedPluginArtifactPath = resolveBundledPluginArtifactPath(pluginArtifactPath)
    return createPlugin(
      pluginLoadPath = pluginArtifactPath,
      pluginArtifactPath = resolvedPluginArtifactPath,
      descriptorPath = descriptorPath,
      resourceResolver = resourceResolver,
      ideVersion = ideVersion,
      layoutComponentName = layoutComponentName,
      classpath = resolvePluginClasspath(resolvedPluginArtifactPath, listOf(pluginArtifactPath).filter(Path::isJar)),
    )
  }

  private fun createPlugin(
    pluginLoadPath: Path,
    pluginArtifactPath: Path,
    descriptorPath: String = PLUGIN_XML,
    resourceResolver: ResourceResolver,
    ideVersion: IdeVersion,
    layoutComponentName: String,
    classpath: Classpath?,
  ): PluginWithArtifactPathResult {
    return try {
      IdePluginManager
        .createManager(resourceResolver)
        .createBundledPlugin(
          pluginFile = pluginLoadPath,
          ideVersion = ideVersion,
          descriptorPath = descriptorPath,
          problemResolver = bundledPluginCreationResultResolver,
          fallbackPluginId = layoutComponentName,
          pluginArtifactPath = pluginArtifactPath,
          classpath = classpath,
        )
        .withPath(pluginArtifactPath)
    } catch (_: PluginFileNotFoundException) {
      PluginWithArtifactPathResult.Failure(pluginArtifactPath, layoutComponentName, MissedFile(pluginArtifactPath.toString()))
    }
  }

  private fun resolvePluginArtifact(classpath: List<Path>): Path {
    val commonParent = getCommonParentDirectory(classpath) ?: return classpath.first()
    val commonParentName = commonParent.fileName?.toString()

    return when {
      commonParentName == LIB_DIRECTORY -> commonParent.parent ?: commonParent
      commonParent.looksLikeJar() && commonParent.parent?.fileName?.toString() == LIB_DIRECTORY -> commonParent.parent.parent ?: commonParent
      else -> commonParent
    }
  }

  private fun resolveBundledPluginArtifactPath(path: Path): Path {
    return if (path.looksLikeJar() && path.parent?.fileName?.toString() == LIB_DIRECTORY) {
      path.parent.parent ?: path
    } else {
      path
    }
  }

  private fun Path.looksLikeJar(): Boolean {
    return fileName?.toString()?.endsWith(".jar", ignoreCase = true) == true
  }

  private fun resolvePluginClasspath(pluginArtifactPath: Path, productInfoClasspathJars: List<Path>): Classpath? {
    val artifactClasspath = contentModuleScanner.getContentModules(pluginArtifactPath).asClasspath()
      .mergeWith(libDirJarsClasspathProvider.getClasspath(pluginArtifactPath))

    if (artifactClasspath.size > 0) {
      return artifactClasspath.getUnique()
    }

    return if (productInfoClasspathJars.isEmpty()) {
      null
    } else {
      Classpath.of(productInfoClasspathJars, ClasspathOrigin.PRODUCT_INFO).getUnique()
    }
  }

  private fun PluginCreationResult<IdePlugin>.withPath(pluginArtifactPath: Path): PluginWithArtifactPathResult = when (this) {
    is PluginCreationSuccess -> PluginWithArtifactPathResult.Success(pluginArtifactPath, plugin)
    is PluginCreationFail -> PluginWithArtifactPathResult.Failure(pluginArtifactPath, errorsAndWarnings)
  }
}
