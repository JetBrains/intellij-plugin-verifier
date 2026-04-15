package com.jetbrains.plugin.structure.ide.resolver

import com.jetbrains.plugin.structure.ide.layout.LayoutComponents
import com.jetbrains.plugin.structure.ide.layout.ResolvedLayoutComponent
import com.jetbrains.plugin.structure.intellij.resources.CompositeResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.JarsResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.NamedResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

private val LOG: Logger = LoggerFactory.getLogger(ProductInfoResourceResolver::class.java)

class ProductInfoResourceResolver(
  layoutComponents: LayoutComponents,
  private val jarFileSystemProvider: JarFileSystemProvider
) : ResourceResolver {

  private val resolvedLayoutComponents = layoutComponents.toList()
  private val resourceResolvers = ConcurrentHashMap<ResolvedLayoutComponent, NamedResourceResolver?>()

  private fun getResourceResolver(layoutComponent: ResolvedLayoutComponent): NamedResourceResolver? =
    resourceResolvers.computeIfAbsent(layoutComponent) {
      if (!layoutComponent.isClasspathable) {
        LOG.debug("No classpath declared for '{}'. Skipping", layoutComponent.layoutComponent)
        return@computeIfAbsent null
      }
      val itemJarResolvers = layoutComponent.resolveClasspaths().map {
        NamedResourceResolver(
          layoutComponent.name + "#" + it.relativePath, JarsResourceResolver(it.toList(), jarFileSystemProvider)
        )
      }
      NamedResourceResolver(layoutComponent.name, CompositeResourceResolver(itemJarResolvers))
    }

  override fun resolveResource(relativePath: String, basePath: Path): ResourceResolver.Result {
    for (layoutComponent in resolvedLayoutComponents) {
      val resolver = getResourceResolver(layoutComponent) ?: continue
      val result = resolver.resolveResource(relativePath, basePath)
      if (result !is ResourceResolver.Result.NotFound) {
        return result
      }
    }
    return ResourceResolver.Result.NotFound
  }
}
