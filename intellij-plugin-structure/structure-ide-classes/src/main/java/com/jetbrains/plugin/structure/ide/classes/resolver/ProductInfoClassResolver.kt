/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.classes.resolver

import com.jetbrains.plugin.structure.base.BinaryClassName
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.EMPTY_RESOLVER
import com.jetbrains.plugin.structure.classes.resolvers.LazyJarResolver
import com.jetbrains.plugin.structure.classes.resolvers.NamedResolver
import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver.ReadMode.FULL
import com.jetbrains.plugin.structure.classes.resolvers.SimpleCompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.asResolver
import com.jetbrains.plugin.structure.ide.BuildTxtIdeVersionProvider
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeVersionResolution
import com.jetbrains.plugin.structure.ide.InvalidIdeException
import com.jetbrains.plugin.structure.ide.ProductInfoAware
import com.jetbrains.plugin.structure.ide.classes.IdeFileOrigin.IdeLibDirectory
import com.jetbrains.plugin.structure.ide.classes.IdeResolverConfiguration
import com.jetbrains.plugin.structure.ide.resolver.LayoutComponentsProvider
import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.platform.ProductInfoParseException
import com.jetbrains.plugin.structure.intellij.platform.ProductInfoParser
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.PluginId
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.objectweb.asm.tree.ClassNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.*

private val LOG: Logger = LoggerFactory.getLogger(ProductInfoClassResolver::class.java)

private const val PRODUCT_INFO_JSON = "product-info.json"
private const val CORE_IDE_PLUGIN_ID = "com.intellij"
private const val PRODUCT_MODULE_V2 = "productModuleV2"
private const val BOOTCLASSPATH_JAR_NAMES = "bootClassPathJarNames"

class ProductInfoClassResolver private constructor(
  private val productInfo: ProductInfo,
  val ide: Ide,
  private val resolverConfiguration: IdeResolverConfiguration
) : NamedResolver("$PRODUCT_INFO_JSON Resolver"), PluginResolverProvider {

  private val layoutComponentsProvider = LayoutComponentsProvider(resolverConfiguration.missingLayoutFileMode)

  private val corePluginClasspathCache: MutableMap<Path, NamedResolver> = HashMap()

  private val _stats: MutableList<String>? = if (resolverConfiguration.isCollectingStats) {
    mutableListOf()
  } else {
    null
  }
  val stats: List<String> get() = _stats ?: emptyList()

  private val resolvers: Map<String, NamedResolver> = mutableMapOf<String, NamedResolver>().also { resolvers ->
    resolveLayout().apply {
      resolvers += resolveCorePlugin()
      resolvers += resolveNonCoreAndNonProductModules()
    }
  }

  override fun getResolver(plugin: IdePlugin): Resolver {
    val id = plugin.pluginId ?: plugin.pluginName
    return id?.let { resolvers[id] } ?: EMPTY_RESOLVER
  }

  override fun contains(pluginId: PluginId) = pluginId in resolvers.keys

  private fun List<LayoutComponentResolver>.resolveCorePlugin(): Map<String, NamedResolver> {
    val corePluginResolver = find { it.name == CORE_IDE_PLUGIN_ID } ?: return emptyMap()
    cacheClasspaths(corePluginResolver)

    val resolvers = mutableMapOf<String, NamedResolver>()

    val productModuleResolvers = filter { it.kind == PRODUCT_MODULE_V2 }
    productModuleResolvers.forEach {
      resolvers.put(it.name, it)
      cacheClasspaths(it)
    }

    val corePluginTreeResolver = CompositeResolver
      .create(listOf(corePluginResolver) + productModuleResolvers, CORE_IDE_PLUGIN_ID)
    resolvers.put(CORE_IDE_PLUGIN_ID, corePluginTreeResolver)

    return resolvers
  }

  private fun List<LayoutComponentResolver>.resolveNonCoreAndNonProductModules(): Map<String, NamedResolver> {
    return filter { it.kind != PRODUCT_MODULE_V2 && it.name != CORE_IDE_PLUGIN_ID }
      .associateBy({ it.name }, { it })
  }

  private val delegateResolver = getDelegateResolver()

  private fun getDelegateResolver(): Resolver = mutableListOf<NamedResolver>().apply {
    add(bootClasspathResolver)
    addAll(resolvers.values)
  }.asResolver("$name delegate")

  private fun resolveLayout(): List<LayoutComponentResolver> =
    layoutComponentsProvider.resolveLayoutComponents(productInfo, ide.idePath)
      .map { it.layoutComponent }
      .map { layoutComponent ->
        if (layoutComponent is LayoutComponent.Classpathable) {
          layoutComponent.toResolver()
        } else {
          LOG.debug("No classpath declared for '{}'. Skipping", layoutComponent)
          layoutComponent.toEmptyResolver()
        }
      }

  val layoutComponentNames: List<String> = resolvers.keys.toList()

  fun getLayoutComponentResolver(name: String): NamedResolver? = resolvers[name]

  private fun LayoutComponent.toEmptyResolver(): LayoutComponentResolver {
    return LayoutComponentResolver(kind, name, resolvers = emptyList(), readMode)
  }

  override val readMode: ReadMode get() = resolverConfiguration.readMode

  @Deprecated("Use 'allClassNames' property instead which is more efficient")
  override val allClasses get() = delegateResolver.allClasses

  override val allClassNames: Set<BinaryClassName> get() = delegateResolver.allClassNames

  @Deprecated("Use 'packages' property instead. This property may be slow on some file systems.")
  override val allPackages get() = delegateResolver.allPackages

  override val packages: Set<String> get() = delegateResolver.packages

  override val allBundleNameSet get() = delegateResolver.allBundleNameSet

  @Deprecated("Use 'resolveClass(BinaryClassName)' instead")
  override fun resolveClass(className: String) = delegateResolver.resolveClass(className)

  override fun resolveClass(className: BinaryClassName): ResolutionResult<ClassNode> {
    return delegateResolver.resolveClass(className)
  }

  override fun resolveExactPropertyResourceBundle(baseName: String, locale: Locale) =
    delegateResolver.resolveExactPropertyResourceBundle(baseName, locale)

  @Deprecated("Use 'containsClass(BinaryClassName)' instead")
  override fun containsClass(className: String) = delegateResolver.containsClass(className)

  override fun containsClass(className: BinaryClassName) = delegateResolver.containsClass(className)

  override fun containsPackage(packageName: String) = delegateResolver.containsPackage(packageName)

  override fun processAllClasses(processor: (ResolutionResult<ClassNode>) -> Boolean) =
    delegateResolver.processAllClasses(processor)

  override fun close() = delegateResolver.close()

  val bootClasspathResolver: NamedResolver
    get() {
      val bootJars = productInfo.launches.firstOrNull()?.bootClassPathJarNames
      return bootJars?.map { getBootJarResolver(it) }.asResolver(BOOTCLASSPATH_JAR_NAMES)
    }

  private fun <C> C.toResolver(): LayoutComponentResolver
    where C : LayoutComponent.Classpathable, C : LayoutComponent {

    return LayoutComponentResolver(kind, name, getClasspathResolvers(), readMode)
  }

  private fun <C> C.getClasspathResolvers(): List<LazyJarResolver>
    where C : LayoutComponent.Classpathable, C : LayoutComponent {
    return getClasspath().map { jarPath: Path ->
      val fullyQualifiedJarFile = this@ProductInfoClassResolver.ide.idePath.resolve(jarPath)
      LazyJarResolver(
        fullyQualifiedJarFile,
        this@ProductInfoClassResolver.readMode,
        IdeLibDirectory(this@ProductInfoClassResolver.ide),
        name = name
      )
    }
  }

  private fun getBootJarResolver(relativeJarPath: String): NamedResolver {
    val fullyQualifiedJarFile = ide.idePath.resolve("lib/$relativeJarPath")
    return corePluginClasspathCache[fullyQualifiedJarFile]?.also {
      recordResolverReuse(fullyQualifiedJarFile)
    } ?: LazyJarResolver(fullyQualifiedJarFile, readMode, IdeLibDirectory(ide), name = relativeJarPath)
  }

  private fun cacheClasspaths(resolver: LayoutComponentResolver) {
    resolver.resolvers.forEach { resolver ->
      corePluginClasspathCache[resolver.jarPath] = resolver
    }
  }

  private fun recordResolverReuse(fullyQualifiedJarFile: Path) {
    _stats?.add("Reusing $fullyQualifiedJarFile")
  }

  companion object {
    @Throws(InvalidIdeException::class)
    fun of(ide: Ide, resolverConfiguration: IdeResolverConfiguration): ProductInfoClassResolver {
      val productInfo = if (ide is ProductInfoAware) {
        ide.productInfo
      } else {
        parseProductInfo(ide)
      }
      return ProductInfoClassResolver(productInfo, ide, resolverConfiguration)
    }

    @Throws(InvalidIdeException::class)
    fun of(ide: Ide, readMode: ReadMode = FULL): ProductInfoClassResolver = of(ide, IdeResolverConfiguration(readMode))

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

    @Throws(InvalidIdeException::class)
    private fun parseProductInfo(ide: Ide): ProductInfo {
      val idePath = ide.idePath
      assertProductInfoPresent(idePath)
      val productInfoParser = ProductInfoParser()
      try {
        return productInfoParser.parse(idePath.productInfoJson)
      } catch (e: ProductInfoParseException) {
        throw InvalidIdeException(idePath, e)
      }
    }

    private fun Path.containsProductInfoJson(): Boolean = resolve(PRODUCT_INFO_JSON).exists()

    private val Path.productInfoJson: Path
      get() {
        return resolve(PRODUCT_INFO_JSON)
      }
  }

  private class LayoutComponentResolver(
    val kind: String,
    name: String,
    override val resolvers: List<LazyJarResolver>,
    readMode: ReadMode,
  ) : SimpleCompositeResolver(resolvers, readMode, name)
}

