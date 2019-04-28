package com.jetbrains.pluginverifier.parameters.classes

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations

/**
 * Allows to specify which classes constitute the [plugin's class loader] [getClassLoader]
 * and which classes should be [verified] [getClassesForCheck].
 */
interface ClassesSelector {

  /**
   * Of [classesLocations] build a [Resolver] that will be
   * used to resolve the classes during the verification.
   */
  fun getClassLoader(classesLocations: IdePluginClassesLocations): Resolver

  /**
   * Selects classes that will be verified. Typically those are a subset
   * of all classes of the [getClassLoader].
   */
  fun getClassesForCheck(classesLocations: IdePluginClassesLocations): Set<String>
}