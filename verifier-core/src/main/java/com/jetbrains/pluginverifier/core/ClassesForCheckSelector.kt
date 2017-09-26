package com.jetbrains.pluginverifier.core

import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.plugin.structure.intellij.classes.locator.CompileServerExtensionKey
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesFinder
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginXmlUtil

/**
 * @author Sergey Patrikeev
 */
class ClassesForCheckSelector {

  private companion object {
    val ADDITIONAL_LOCATIONS_KEYS = listOf(CompileServerExtensionKey)
  }

  /**
   * Determines plugin's classes that must be verified.
   *
   * The purpose of this method is to filter out the unrelated classes which
   * don't use the IntelliJ API: many plugins pack the libraries without all transitive dependencies.
   * We don't want to check such classes because it leads to false warnings.
   *
   * Instead, our approach is to look at classes referenced in the `plugin.xml`. Given these classes
   * we predict the .jar-files that correspond to the plugin itself (not the secondary bundled library).
   * The additional .jar-s which are explicitly specified in the `plugin.xml`
   * (such as those defined by `compileServer.plugin` extension point) are to be verified, too.
   */
  fun getClassesForCheck(classesLocations: IdePluginClassesLocations): Iterator<String> {
    val mainResolvers = IdePluginClassesFinder.MAIN_CLASSES_KEYS
        .mapNotNull { classesLocations.getResolver(it) }
        .flatMap { it.finalResolvers }

    val mainUnitedResolver = UnionResolver.create(mainResolvers)
    val referencedResolvers = getAllClassesReferencedFromXml(classesLocations.idePlugin)
        .mapNotNull { mainUnitedResolver.getClassLocation(it) }
        .let { if (it.isEmpty()) mainResolvers else it }

    val additionalResolvers = ADDITIONAL_LOCATIONS_KEYS
        .mapNotNull { classesLocations.getResolver(it) }
        .flatMap { it.finalResolvers }

    return UnionResolver.create(referencedResolvers + additionalResolvers).allClasses
  }

  private fun getAllClassesReferencedFromXml(plugin: IdePlugin): Set<String> =
      PluginXmlUtil.getAllClassesReferencedFromXml(plugin) +
          plugin.optionalDescriptors.flatMap { PluginXmlUtil.getAllClassesReferencedFromXml(it.value) }


}