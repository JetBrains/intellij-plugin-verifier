package com.jetbrains.plugin.structure.intellij.classes.locator

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.io.File

interface ClassesLocator {
  val locationKey: LocationKey

  fun findClasses(idePlugin: IdePlugin, pluginFile: File): List<Resolver>
}