package com.jetbrains.plugin.structure.intellij.resources

import com.jetbrains.plugin.structure.intellij.utils.URLUtil
import java.io.File
import java.net.URL

class CompiledModulesResourceResolver(private val moduleRoots: List<File>) : ResourceResolver {
  override fun resolveResource(relativePath: String, base: URL): ResourceResolver.Result {
    val defaultResolve = DefaultResourceResolver.resolveResource(relativePath, base)
    if (defaultResolve !is ResourceResolver.Result.NotFound) {
      return defaultResolve
    }

    //Try to resolve path against module roots. [base] is ignored.
    val moduleRootRelativePath = if (relativePath.startsWith("/")) {
      relativePath.substringAfter("/")
    } else {
      "META-INF/" + relativePath.substringAfter("./")
    }

    for (moduleRoot in moduleRoots) {
      val file = moduleRoot.resolve(moduleRootRelativePath)
      if (file.exists()) {
        val url = URLUtil.fileToUrl(file)
        return try {
          val stream = file.inputStream().buffered()
          ResourceResolver.Result.Found(url, stream)
        } catch (e: Exception) {
          ResourceResolver.Result.Failed(url, e)
        }
      }
    }

    return ResourceResolver.Result.NotFound
  }
}
