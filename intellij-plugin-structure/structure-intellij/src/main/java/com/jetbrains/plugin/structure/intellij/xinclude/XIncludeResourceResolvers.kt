package com.jetbrains.plugin.structure.intellij.xinclude

import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.jar.META_INF
import java.nio.file.FileSystems
import java.nio.file.Path

/**
 * Resolves a resource not only in the relative path, but also in the `META-INF` subdirectory.
 *
 * This is used to resolve plugin descriptors that are scattered across resource roots and `META-INF`
 * directories, while XIncluding each other.
 */
class MetaInfResourceResolver(private val delegateResolver: ResourceResolver) : ResourceResolver {
  override fun resolveResource(relativePath: String, basePath: Path): ResourceResolver.Result {
    val parentPath = getParent(basePath) ?: return ResourceResolver.Result.NotFound
    return delegateResolver.resolveResource(relativePath, parentPath.resolve(META_INF).resolve(relativePath))
  }


  private fun getParent(path: Path): Path? {
    val parent: Path? = path.parent
    return if (parent == null && path.fileSystem != FileSystems.getDefault()) {
      path.fileSystem.rootDirectories.first { root -> root == path.root }
    } else {
      parent
    }
  }
}

/**
 * Resolves a resource not only in the base path, but also in the parent directory.
 *
 * This is used to resolve a XIncluded resource placed in the root directory being referenced
 * from the `META-INF` directory.
 *
 */
class InParentPathResourceResolver(private val delegateResolver: ResourceResolver) : ResourceResolver {
  override fun resolveResource(relativePath: String, basePath: Path): ResourceResolver.Result {
    val parentPath: Path = basePath.parent ?: return ResourceResolver.Result.NotFound
    return delegateResolver.resolveResource(relativePath, parentPath)
  }
}