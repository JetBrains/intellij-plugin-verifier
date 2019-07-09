package com.jetbrains.pluginverifier.parameters.classes

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations

/**
 * Allows to specify which classes constitute the [plugin's class loader] [getClassLoader]
 * and which classes should be [verified] [getClassesForCheck].
 */
interface ClassesSelector {

  fun getClassLoader(classesLocations: IdePluginClassesLocations): List<Resolver>

  fun getClassesForCheck(classesLocations: IdePluginClassesLocations): Set<String>
}