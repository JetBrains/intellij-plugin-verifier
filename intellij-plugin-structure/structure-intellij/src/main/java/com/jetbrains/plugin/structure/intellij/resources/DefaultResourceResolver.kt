package com.jetbrains.plugin.structure.intellij.resources

import com.jetbrains.plugin.structure.intellij.utils.ThreeState
import com.jetbrains.plugin.structure.intellij.utils.URLUtil
import java.net.MalformedURLException
import java.net.URL

object DefaultResourceResolver : ResourceResolver {
  override fun resolveResource(relativePath: String, base: URL): ResourceResolver.Result {
    val url = try {
      URL(base, relativePath)
    } catch (e: MalformedURLException) {
      return ResourceResolver.Result.NotFound
    }
    if (URLUtil.resourceExists(url) == ThreeState.YES) {
      return try {
        val stream = URLUtil.openStream(url)
        ResourceResolver.Result.Found(url, stream)
      } catch (e: Exception) {
        ResourceResolver.Result.Failed(url, e)
      }
    }
    return ResourceResolver.Result.NotFound
  }
}
