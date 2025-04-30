/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.filtering

import com.jetbrains.plugin.structure.base.BinaryClassName
import com.jetbrains.plugin.structure.base.utils.binaryClassNames
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.classes.locator.LocationKey
import com.jetbrains.plugin.structure.intellij.classes.plugin.BundledPluginClassesFinder
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesFinder
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginXmlUtil

/**
 * [ClassesSelector] that selects for verification all the classes that constitute the
 * plugin's class loader in IDEA, except for the bundled libraries' classes:
 * 1) all classes referenced in the `plugin.xml`.
 * 1) for .jar-red plugins, all the classes contained in `.jar`;
 * 2) for .zip-ped plugins, all the classes from the `/lib` directory and from the `/classes` directory.
 *
 * Note that this selector tries to ignore third-party classes from libraries bundled into the plugin distribution because
 * a) they typically don't contain IntelliJ API usages
 * b) the verification may produce false warnings since some libraries optionally depend on missing libraries.
 */
class MainClassesSelector private constructor(private val locationKeys: List<LocationKey>) : ClassesSelector {

  companion object {
    fun forPlugin(): MainClassesSelector {
      return MainClassesSelector(IdePluginClassesFinder.MAIN_CLASSES_KEYS)
    }
    fun forBundledPlugin(): MainClassesSelector {
      return MainClassesSelector(BundledPluginClassesFinder.LOCATION_KEYS)
    }
  }

  /**
   * Selects the plugin's classes that can be referenced by the plugin and its dependencies.
   */
  override fun getClassLoader(classesLocations: IdePluginClassesLocations): List<Resolver> =
    locationKeys.flatMap { classesLocations.getResolvers(it) }

  /**
   * Determines plugin's classes that must be verified.
   *
   * The purpose of this method is to filter out unrelated classes which
   * don't use the IntelliJ API: many plugins pack the libraries without all transitive dependencies.
   * We don't want to check such classes because it leads to false warnings.
   *
   * Instead, our approach is to look at classes referenced in the `plugin.xml`. Given these classes
   * we predict the .jar-files that correspond to the plugin itself (not the secondary bundled libraries).
   */
  override fun getClassesForCheck(classesLocations: IdePluginClassesLocations): Set<BinaryClassName> {
    val resolvers = locationKeys.flatMap { classesLocations.getResolvers(it) }

    val allClassesReferencedFromXml = getAllClassesReferencedFromXml(classesLocations.idePlugin)

    val referencedResolvers = resolvers.filter { resolver ->
      allClassesReferencedFromXml.any { className -> resolver.containsClass(className) }
    }

    val checkResolvers = if (referencedResolvers.isEmpty()) {
      resolvers
    } else {
      referencedResolvers
    }

    return checkResolvers.flatMapTo(binaryClassNames()) { it.allClassNames }
  }

  private fun getAllClassesReferencedFromXml(plugin: IdePlugin): Set<String> {
    return PluginXmlUtil.getAllClassesReferencedFromXml(plugin) +
      plugin.optionalDescriptors.flatMap { PluginXmlUtil.getAllClassesReferencedFromXml(it.optionalPlugin) } +
      plugin.modulesDescriptors.flatMap { PluginXmlUtil.getAllClassesReferencedFromXml(it.module) }
  }


}