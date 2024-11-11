package com.jetbrains.plugin.structure.ide.resolver

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.plugin.JarFilesResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.CompositeResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.NamedResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

private val LOG: Logger = LoggerFactory.getLogger(ProductInfoResourceResolver::class.java)

class ProductInfoResourceResolver(
  productInfo: ProductInfo,
  idePath: Path,
  private val excludeMissingProductInfoLayoutComponents: Boolean = true
) : ResourceResolver {

  private val delegateResolver = getPlatformResourceResolver(resolveLayoutComponents(productInfo, idePath))

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
  private fun resolveLayoutComponents(productInfo: ProductInfo, idePath: Path): LayoutComponents {
    val layoutComponents = LayoutComponents.of(idePath, productInfo)
    return if (excludeMissingProductInfoLayoutComponents) {
      val (okComponents, failedComponents) = layoutComponents.partition { it.allClasspathsExist() }
      logUnavailableClasspath(failedComponents)
      LayoutComponents(okComponents)
    } else {
      layoutComponents
    }
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

  override fun resolveResource(relativePath: String, basePath: Path): ResourceResolver.Result =
    delegateResolver.resolveResource(relativePath, basePath)

  private fun logUnavailableClasspath(failedComponents: List<ResolvedLayoutComponent>) {
    val logMsg = failedComponents.joinToString("\n") {
      val cp = it.getClasspaths().joinToString(", ")
      "Layout component '${it.name}' has some nonexistent 'classPath' elements: '$cp'"
    }
    LOG.atWarn().log(logMsg)
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

  private class LayoutComponents(val layoutComponents: List<ResolvedLayoutComponent>) :
    Iterable<ResolvedLayoutComponent> {
    companion object {
      fun of(idePath: Path, productInfo: ProductInfo): LayoutComponents {
        val resolvedLayoutComponents = productInfo.layout
          .map { ResolvedLayoutComponent(idePath, it) }
        return LayoutComponents(resolvedLayoutComponents)
      }
    }

    override fun iterator() = layoutComponents.iterator()
  }
}
