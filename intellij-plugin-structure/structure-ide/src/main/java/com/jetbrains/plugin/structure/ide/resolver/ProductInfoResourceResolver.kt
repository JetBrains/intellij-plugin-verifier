package com.jetbrains.plugin.structure.ide.resolver

import com.jetbrains.plugin.structure.ide.layout.LayoutComponents
import com.jetbrains.plugin.structure.ide.layout.ResolvedLayoutComponent
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.resources.CompositeResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.JarsResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.NamedResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

private val LOG: Logger = LoggerFactory.getLogger(ProductInfoResourceResolver::class.java)

class ProductInfoResourceResolver(
  productInfo: ProductInfo,
  idePath: Path,
  layoutComponentsProvider: LayoutComponentsProvider,
  private val jarFileSystemProvider: JarFileSystemProvider
) : ResourceResolver {

  private val delegateResolver = getPlatformResourceResolver(layoutComponentsProvider.resolveLayoutComponents(productInfo, idePath))

  private fun getPlatformResourceResolver(layoutComponents: LayoutComponents): CompositeResourceResolver {
    val resourceResolvers = layoutComponents.mapNotNull {
      if (it.isClasspathable) {
        getResourceResolver(it)
      } else {
        LOG.debug("No classpath declared for '{}'. Skipping", it.layoutComponent)
        null
      }
    }
    return CompositeResourceResolver(resourceResolvers)
  }

  private fun getResourceResolver(layoutComponent: ResolvedLayoutComponent): NamedResourceResolver? {
    if (!layoutComponent.isClasspathable) {
      return null
    }
    val itemJarResolvers = layoutComponent.resolveClasspaths().map {
      NamedResourceResolver(
        layoutComponent.name + "#" + it.relativePath, JarsResourceResolver(it.toList(), jarFileSystemProvider)
      )
    }
    return NamedResourceResolver(layoutComponent.name, CompositeResourceResolver(itemJarResolvers))
  }

  override fun resolveResource(relativePath: String, basePath: Path): ResourceResolver.Result =
    delegateResolver.resolveResource(relativePath, basePath)


}
