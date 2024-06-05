/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.utils.description
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.inputStream
import com.jetbrains.plugin.structure.base.utils.withZipFsSeparator
import com.jetbrains.plugin.structure.intellij.resources.DefaultResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import java.nio.file.FileSystems
import java.nio.file.Path

class JarFilesResourceResolver(private val jarFiles: List<Path>) : ResourceResolver {

  override fun resolveResource(relativePath: String, basePath: Path): ResourceResolver.Result {
    val resourceResult = DefaultResourceResolver.resolveResource(relativePath, basePath)
    if (resourceResult !is ResourceResolver.Result.NotFound) {
      return resourceResult
    }
    val finalPath = basePath.resolveSibling(relativePath).toString()
    for (jarFile in jarFiles) {
      val jarFs = FileSystems.newFileSystem(jarFile, JarFilesResourceResolver::class.java.classLoader)
      val foundResult = try {
        val path = jarFs.getPath(finalPath.withZipFsSeparator())
        if (path.exists()) {
          ResourceResolver.Result.Found(path, path.inputStream(), resourceToClose = jarFs, description = jarFile.description)
        } else {
          null
        }
      } catch (e: Throwable) {
        jarFs.close()
        throw e
      }
      if (foundResult != null) {
        return foundResult
      }
      jarFs.close()
    }
    return ResourceResolver.Result.NotFound
  }
}
