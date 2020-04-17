/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.resources

import java.net.URL

class CompositeResourceResolver(private val resolvers: List<ResourceResolver>) : ResourceResolver {
  override fun resolveResource(relativePath: String, base: URL): ResourceResolver.Result {
    for (resolver in resolvers) {
      val resourceResult = resolver.resolveResource(relativePath, base)
      if (resourceResult !is ResourceResolver.Result.NotFound) {
        return resourceResult
      }
    }
    return ResourceResolver.Result.NotFound
  }
}
