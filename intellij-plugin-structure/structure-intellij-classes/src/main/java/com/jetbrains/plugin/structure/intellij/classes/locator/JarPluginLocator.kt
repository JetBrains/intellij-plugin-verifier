package com.jetbrains.plugin.structure.intellij.classes.locator

import com.jetbrains.plugin.structure.base.utils.FileUtil
import com.jetbrains.plugin.structure.classes.resolvers.JarFileResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.io.File

class JarPluginLocator : IdePluginClassesLocator {
  override val locationKey: LocationKey = JarPluginKey

  override fun findClasses(idePlugin: IdePlugin, pluginFile: File): Resolver? {
    if (FileUtil.isJar(pluginFile)) {
      return JarFileResolver(pluginFile)
    }
    return null
  }
}