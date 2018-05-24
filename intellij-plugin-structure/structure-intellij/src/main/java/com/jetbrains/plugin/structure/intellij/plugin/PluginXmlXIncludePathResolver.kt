package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.base.utils.isZip
import com.jetbrains.plugin.structure.intellij.utils.ThreeState
import com.jetbrains.plugin.structure.intellij.utils.URLUtil
import com.jetbrains.plugin.structure.intellij.utils.xincludes.DefaultXIncludePathResolver
import com.jetbrains.plugin.structure.intellij.utils.xincludes.XIncludeException
import java.io.File
import java.net.MalformedURLException
import java.net.URL

class PluginXmlXIncludePathResolver(files: List<File>) : DefaultXIncludePathResolver() {

  private val metaInfUrls = getMetaInfUrls(files)

  private fun getMetaInfUrls(files: List<File>) =
      files.asSequence()
          .filter { it.isJar() || it.isZip() }
          .mapNotNull {
            try {
              URLUtil.getJarEntryURL(it, "${IdePluginManager.META_INF}/")
            } catch (e: MalformedURLException) {
              null
            }
          }.toList()

  private fun defaultResolve(relativePath: String, base: String?) =
      if (base != null && relativePath.startsWith("/${IdePluginManager.META_INF}/")) {
        try {
          URL(URL(base), "..$relativePath")
        } catch (e: MalformedURLException) {
          throw XIncludeException(e)
        }
      } else {
        super.resolvePath(relativePath, base)
      }

  private fun getRelativeUrl(base: URL, path: String) =
      if (path.startsWith("/")) {
        URL(base, "..$path")
      } else {
        URL(base, path)
      }

  override fun resolvePath(relativePath: String, base: String?): URL =
      metaInfUrls.asSequence()
          .mapNotNull {
            try {
              getRelativeUrl(it, relativePath)
            } catch (e: MalformedURLException) {
              null
            }
          }
          .find { URLUtil.resourceExists(it) == ThreeState.YES }
          ?: defaultResolve(relativePath, base)
}
