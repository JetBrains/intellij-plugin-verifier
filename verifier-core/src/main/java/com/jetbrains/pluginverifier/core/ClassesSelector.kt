package com.jetbrains.pluginverifier.core

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations

interface ClassesSelector {

  fun getClassLoader(classesLocations: IdePluginClassesLocations): Resolver

  fun getClassesForCheck(classesLocations: IdePluginClassesLocations): Set<String>
}