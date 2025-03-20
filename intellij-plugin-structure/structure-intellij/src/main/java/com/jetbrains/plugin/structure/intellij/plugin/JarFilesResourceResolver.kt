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
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

class JarFilesResourceResolver(private val jarFiles: List<Path>) : ResourceResolver {

  // final path ⇒ path to the first JAR file having the particular resource
  // None ⇒ not found and never will be
  private val cache = ConcurrentHashMap<String, Optional<Path>>()

  override fun resolveResource(relativePath: String, basePath: Path): ResourceResolver.Result {
    val resourceResult = DefaultResourceResolver.resolveResource(relativePath, basePath)
    if (resourceResult !is ResourceResolver.Result.NotFound) {
      return resourceResult
    }
    val finalPath = basePath.resolveSibling(relativePath).toString()
    val cachedJarPath = cache[finalPath]
    if (cachedJarPath != null) {
      println("Cache reused for path finalPath=$finalPath, cachedJarPath=$cachedJarPath")
      return if (cachedJarPath.isPresent)
        loadFromJar(cachedJarPath.get(), finalPath)!!
      else
        ResourceResolver.Result.NotFound
    }

    for (jarFile in jarFiles) {
      val result = loadFromJar(jarFile, finalPath)
      if (result != null) {
        println("Cache filled for path finalPath=$finalPath, path=$jarFile")
        cache[finalPath] = Optional.of(jarFile)
        return result
      }
    }

    println("Cache not found for path finalPath=$finalPath, path=NOT FOUND")
    cache[finalPath] = Optional.empty<Path>()
    return ResourceResolver.Result.NotFound
  }
}

private fun loadFromJar(jarFile: Path, finalPath: String): ResourceResolver.Result? {
  val jarFs = FileSystems.newFileSystem(jarFile, JarFilesResourceResolver::class.java.classLoader)
  val foundResult = try {
    val path = jarFs.getPath(finalPath.withZipFsSeparator())
    if (path.exists()) {
      ResourceResolver.Result.Found(
        path,
        path.inputStream(),
        resourceToClose = jarFs,
        description = jarFile.description
      )
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
  return null
}
