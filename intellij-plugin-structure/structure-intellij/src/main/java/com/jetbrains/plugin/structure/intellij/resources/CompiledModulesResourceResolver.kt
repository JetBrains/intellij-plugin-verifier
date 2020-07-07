/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.resources

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.inputStream
import java.net.URL
import java.nio.file.Path

class CompiledModulesResourceResolver(private val moduleRoots: List<Path>) : ResourceResolver {
  override fun resolveResource(relativePath: String, basePath: Path): ResourceResolver.Result {
    val defaultResolve = DefaultResourceResolver.resolveResource(relativePath, basePath)
    if (defaultResolve !is ResourceResolver.Result.NotFound) {
      return defaultResolve
    }

    //Try to resolve path against module roots. [base] is ignored.
    val moduleRootRelativePath = if (relativePath.startsWith("/")) {
      relativePath.trimStart('/')
    } else {
      "META-INF/" + if (relativePath.startsWith("./")) relativePath.substringAfter("./") else relativePath
    }

    for (moduleRoot in moduleRoots) {
      val file = moduleRoot.resolve(moduleRootRelativePath)
      if (file.exists()) {
        return try {
          ResourceResolver.Result.Found(file, file.inputStream())
        } catch (e: Exception) {
          ResourceResolver.Result.Failed(file, e)
        }
      }
    }

    return ResourceResolver.Result.NotFound
  }
}
