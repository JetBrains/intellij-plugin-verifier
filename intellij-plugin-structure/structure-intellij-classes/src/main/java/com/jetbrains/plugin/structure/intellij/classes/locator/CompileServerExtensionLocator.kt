package com.jetbrains.plugin.structure.intellij.classes.locator

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.buildJarFileResolvers
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.io.File

/**
 * Classes that added to external build process' classpath.
 * See PR-1063 and com.intellij.compiler.server.CompileServerPlugin for details
 */
class CompileServerExtensionLocator(private val readMode: Resolver.ReadMode) : ClassesLocator {
  companion object {
    private const val EXTENSION_POINT_NAME = "com.intellij.compileServer.plugin"
  }

  override val locationKey = CompileServerExtensionKey

  override fun findClasses(idePlugin: IdePlugin, pluginFile: File): List<Resolver> {
    val pluginLib = pluginFile.resolve("lib")
    if (pluginLib.isDirectory) {
      val elements = idePlugin.extensions.get(EXTENSION_POINT_NAME)
      val allCompileJars = elements
          .mapNotNull { it.getAttributeValue("classpath") }
          .flatMap { it.split(";") }
          .filter { it.endsWith(".jar") }
          .map { File(pluginLib, it) }
          .filter { it.isFile }
      return buildJarFileResolvers(allCompileJars, readMode, PluginClassFileOrigin.CompileServer(idePlugin))
    }
    return emptyList()
  }
}

object CompileServerExtensionKey : LocationKey {
  override val name: String = "compileServer.plugin extension point"

  override fun getLocator(readMode: Resolver.ReadMode) = CompileServerExtensionLocator(readMode)

}