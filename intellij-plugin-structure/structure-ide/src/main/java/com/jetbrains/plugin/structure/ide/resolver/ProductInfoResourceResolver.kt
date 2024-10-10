package com.jetbrains.plugin.structure.ide.resolver

import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.plugin.JarFilesResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.CompositeResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.NamedResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.asResolver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

private val LOG: Logger = LoggerFactory.getLogger(ProductInfoResourceResolver::class.java)

class ProductInfoResourceResolver(productInfo: ProductInfo, private val idePath: Path) : ResourceResolver {

  private val delegateResolver = productInfo.layout
    .mapNotNull(::getResourceResolver)
    .asResolver()

  private fun getResourceResolver(layoutComponent: LayoutComponent): NamedResourceResolver? {
    return if (layoutComponent is LayoutComponent.Classpathable) {
      val itemJarResolvers = layoutComponent.getClasspath().map { jarPath: Path ->
        val fullyQualifiedJarFile = idePath.resolve(jarPath)
        NamedResourceResolver(layoutComponent.name + "#" + jarPath, JarFilesResourceResolver(listOf(fullyQualifiedJarFile)))
      }
      NamedResourceResolver(layoutComponent.name, CompositeResourceResolver(itemJarResolvers))
    } else {
      LOG.atDebug().log("No classpath declared for '{}'. Skipping", layoutComponent)
      null
    }
  }

  override fun resolveResource(relativePath: String, basePath: Path): ResourceResolver.Result =
    delegateResolver.resolveResource(relativePath, basePath)
}