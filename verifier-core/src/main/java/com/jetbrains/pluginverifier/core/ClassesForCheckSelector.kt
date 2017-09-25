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

  fun getClassesForCheck(plugin: IdePlugin, locations: IdePluginClassesLocations): Iterator<String> {
    val mainUnitedResolver = UnionResolver.create(IdePluginClassesFinder.MAIN_CLASSES_KEYS.mapNotNull { locations.getResolver(it) })
    val referencedResolvers = getAllClassesReferencedFromXml(plugin).mapNotNull { mainUnitedResolver.getClassLocation(it) }

    val additionalResolvers = ADDITIONAL_LOCATIONS_KEYS.mapNotNull { locations.getResolver(it) }

    val result = UnionResolver.create((referencedResolvers + additionalResolvers).distinct())
    return if (result.isEmpty) locations.getUnitedResolver().allClasses else result.allClasses
  }

  private fun getAllClassesReferencedFromXml(plugin: IdePlugin): Set<String> =
      PluginXmlUtil.getAllClassesReferencedFromXml(plugin) +
          plugin.optionalDescriptors.flatMap { PluginXmlUtil.getAllClassesReferencedFromXml(it.value) }


}