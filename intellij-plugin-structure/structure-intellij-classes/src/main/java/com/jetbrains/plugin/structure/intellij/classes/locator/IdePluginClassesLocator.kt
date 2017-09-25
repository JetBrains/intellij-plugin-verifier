package com.jetbrains.plugin.structure.intellij.classes.locator

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.io.File

/**
 * @author Sergey Patrikeev
 */
interface IdePluginClassesLocator {
  val locationKey: LocationKey

  fun findClasses(idePlugin: IdePlugin, pluginFile: File): Resolver?
}