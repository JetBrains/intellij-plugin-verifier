package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.utils.FileUtil
import com.jetbrains.plugin.structure.intellij.utils.URLUtil
import com.jetbrains.plugin.structure.intellij.utils.xincludes.DefaultXIncludePathResolver
import com.jetbrains.plugin.structure.intellij.utils.xincludes.XIncludeException
import org.jetbrains.annotations.Nullable
import java.io.File
import java.net.MalformedURLException
import java.net.URL

class PluginXmlXIncludePathResolver(files: List<File>) : DefaultXIncludePathResolver() {

  private val metaInfUrls = getMetaInfUrls(files)

  private fun getMetaInfUrls(files: List<File>) =
      files.asSequence()
          .filter { FileUtil.isJar(it) || FileUtil.isZip(it) }
          .mapNotNull {
            try {
              URLUtil.getJarEntryURL(it, "META-INF/")
            } catch (e: MalformedURLException) {
              null
            }
          }.toList()

  private fun defaultResolve(relativePath: String, @Nullable base: String?): URL {
    return if (base != null && relativePath.startsWith("/META-INF/")) {
      try {
        URL(URL(base), ".." + relativePath)
      } catch (e: MalformedURLException) {
        throw XIncludeException(e)
      }
    } else {
      super.resolvePath(relativePath, base)
    }
  }

  private fun getRelativeUrl(base: URL, path: String) =
      if (path.startsWith("/")) {
        URL(base, ".." + path)
      } else {
        URL(base, path)
      }

  override fun resolvePath(relativePath: String, base: String?): URL {
    val url = defaultResolve(relativePath, base)
    if (URLUtil.resourceExists(url)) {
      return url
    }

    return metaInfUrls.asSequence()
        .mapNotNull {
          try {
            getRelativeUrl(it, relativePath)
          } catch (e: MalformedURLException) {
            null
          }
        }
        .filter { URLUtil.resourceExists(it) }
        .firstOrNull()
        ?: url
  }
}
