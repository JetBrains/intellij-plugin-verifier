package com.jetbrains.plugin.structure.ide.classes.resolver

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.classes.resolvers.JarFileResolver
import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver.ReadMode.FULL
import com.jetbrains.plugin.structure.ide.BuildTxtIdeVersionProvider
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeVersionResolution
import com.jetbrains.plugin.structure.ide.InvalidIdeException
import com.jetbrains.plugin.structure.ide.classes.IdeFileOrigin.IdeLibDirectory
import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.platform.ProductInfoParseException
import com.jetbrains.plugin.structure.intellij.platform.ProductInfoParser
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.objectweb.asm.tree.ClassNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.*

private val LOG: Logger = LoggerFactory.getLogger(ProductInfoClassResolver::class.java)

private const val PRODUCT_INFO_JSON = "product-info.json"

class ProductInfoClassResolver(
  private val productInfo: ProductInfo, val ide: Ide, override val readMode: ReadMode = FULL
) : Resolver() {

  val layoutComponentResolvers: List<NamedResolver> =  productInfo.layout
    .mapNotNull(::getResourceResolver)

  private val delegateResolver = bootClasspathResolver + layoutComponentResolvers

  private fun getResourceResolver(layoutComponent: LayoutComponent): NamedResolver? {
    return if (layoutComponent is LayoutComponent.Classpathable) {
      getClasspathableResolver(layoutComponent)
    } else {
      LOG.atDebug().log("No classpath declared for '{}'. Skipping", layoutComponent)
      null
    }
  }

  override val allClasses get() = delegateResolver.allClasses

  override val allPackages get() = delegateResolver.allPackages

  override val allBundleNameSet get() = delegateResolver.allBundleNameSet

  override fun resolveClass(className: String) = delegateResolver.resolveClass(className)

  override fun resolveExactPropertyResourceBundle(baseName: String, locale: Locale) =
    delegateResolver.resolveExactPropertyResourceBundle(baseName, locale)

  override fun containsClass(className: String) = delegateResolver.containsClass(className)

  override fun containsPackage(packageName: String) = delegateResolver.containsPackage(packageName)

  override fun processAllClasses(processor: (ResolutionResult<ClassNode>) -> Boolean) =
    delegateResolver.processAllClasses(processor)

  override fun close() = delegateResolver.close()

  val bootClasspathResolver: NamedResolver
    get() {
      val bootJars = productInfo.launches.firstOrNull()?.bootClassPathJarNames
      val bootResolver = bootJars?.map { getBootJarResolver(it) }?.asResolver()
        ?: EmptyResolver
      return NamedResolver("bootClassPathJarNames", bootResolver)
    }

  private fun <C> getClasspathableResolver(layoutComponent: C): NamedResolver
    where C : LayoutComponent.Classpathable, C : LayoutComponent {
    val itemJarResolvers = layoutComponent.getClasspath().map { jarPath: Path ->
      val fullyQualifiedJarFile = ide.idePath.resolve(jarPath)
      NamedResolver(
        layoutComponent.name + "#" + jarPath,
        JarFileResolver(fullyQualifiedJarFile, readMode, IdeLibDirectory(ide))
      )
    }
    return NamedResolver(layoutComponent.name, CompositeResolver.create(itemJarResolvers))
  }

  private fun getBootJarResolver(relativeJarPath: String): NamedResolver {
    val fullyQualifiedJarFile = ide.idePath.resolve("lib/$relativeJarPath")
    return NamedResolver(relativeJarPath, JarFileResolver(fullyQualifiedJarFile, readMode, IdeLibDirectory(ide)))
  }

  private operator fun Resolver.plus(moreResolvers: Collection<NamedResolver>) =
    CompositeResolver.create(listOf(this) + moreResolvers)

  private fun List<NamedResolver>.asResolver() = CompositeResolver.create(this)

  companion object {
    @Throws(InvalidIdeException::class)
    fun of(ide: Ide, readMode: ReadMode = FULL): ProductInfoClassResolver {
      val idePath = ide.idePath
      assertProductInfoPresent(idePath)
      val productInfoParser = ProductInfoParser()
      try {
        val productInfo = productInfoParser.parse(idePath.productInfoJson)
        return ProductInfoClassResolver(productInfo, ide, readMode)
      } catch (e: ProductInfoParseException) {
        throw InvalidIdeException(idePath, e)
      }
    }

    @Throws(InvalidIdeException::class)
    private fun assertProductInfoPresent(idePath: Path) {
      if (!idePath.containsProductInfoJson()) {
        throw InvalidIdeException(idePath, "The '${PRODUCT_INFO_JSON}' file is not available.")
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

    private fun Path.containsProductInfoJson(): Boolean = resolve(PRODUCT_INFO_JSON).exists()

    private val Path.productInfoJson: Path
      get() {
        return resolve(PRODUCT_INFO_JSON)
      }
  }
}