/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.resources.DefaultResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.utils.ThreeState
import com.jetbrains.plugin.structure.intellij.utils.URLUtil
import java.io.File
import java.net.URL

class JarFilesResourceResolver(private val jarFiles: List<File>) : ResourceResolver {
  override fun resolveResource(relativePath: String, base: URL): ResourceResolver.Result {
    val resourceResult = DefaultResourceResolver.resolveResource(relativePath, base)
    if (resourceResult !is ResourceResolver.Result.NotFound) {
      return resourceResult
    }
    for (jarFile in jarFiles) {
      val url = URLUtil.getJarEntryURL(jarFile, relativePath)
      if (URLUtil.resourceExists(url) == ThreeState.YES) {
        return try {
          val stream = URLUtil.openStream(url)
          ResourceResolver.Result.Found(url, stream)
        } catch (e: Exception) {
          ResourceResolver.Result.Failed(url, e)
        }
      }
    }
    return ResourceResolver.Result.NotFound
  }
}
