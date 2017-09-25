package com.jetbrains.plugin.structure.intellij.classes.locator

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.utils.JarsUtils
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.io.File

/**
 * Classes that added to external build process' classpath.
 * See PR-1063 and com.intellij.compiler.server.CompileServerPlugin for details
 */
class CompileServerExtensionLocator : ClassesLocator {
  companion object {
    private val EXTENSION_POINT_NAME = "com.intellij.compileServer.plugin"
  }

  override val locationKey: LocationKey = CompileServerExtensionKey

  override fun findClasses(idePlugin: IdePlugin, pluginFile: File): Resolver? {
    val pluginLib = File(pluginFile, "lib")
    if (pluginLib.isDirectory) {
      val elements = idePlugin.extensions.get(EXTENSION_POINT_NAME)
      val allCompileJars = elements
          .mapNotNull { it.getAttributeValue("classpath") }
          .flatMap { it.split(";") }
          .filter { it.endsWith(".jar") }
          .map { File(pluginLib, it) }
          .filter { it.isFile }
      return JarsUtils.makeResolver(allCompileJars)
    }
    return null
  }
}

object CompileServerExtensionKey : LocationKey {
  override val name: String = "compileServer.plugin extension point"

  override val locator: ClassesLocator = CompileServerExtensionLocator()
}