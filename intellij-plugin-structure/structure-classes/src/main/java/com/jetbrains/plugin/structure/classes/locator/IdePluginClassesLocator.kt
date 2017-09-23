package com.jetbrains.plugin.structure.classes.locator

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.io.File
import java.io.IOException

/**
 * @author Sergey Patrikeev
 */
interface IdePluginClassesLocator {
  @Throws(IOException::class)
  fun findClasses(idePlugin: IdePlugin, pluginDirectory: File): List<Resolver>
}