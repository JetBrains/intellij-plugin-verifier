package com.jetbrains.plugin.structure.intellij.utils.xincludes

import com.jetbrains.plugin.structure.intellij.utils.ThreeState
import com.jetbrains.plugin.structure.intellij.utils.URLUtil
import java.net.URL

class CompositeXIncludePathResolver(private val resolvers: List<XIncludePathResolver>) : XIncludePathResolver {
  override fun resolvePath(relativePath: String, base: String?): URL =
      resolvers
          .asSequence()
          .mapNotNull { it.resolvePath(relativePath, base) }
          .firstOrNull { URLUtil.resourceExists(it) == ThreeState.YES }
          ?: throw XIncludeException("Unresolved relative '$relativePath' against '$base'")
}
