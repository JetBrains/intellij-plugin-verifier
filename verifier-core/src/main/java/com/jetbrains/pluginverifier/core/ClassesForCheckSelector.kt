package com.jetbrains.pluginverifier.core

import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.plugin.structure.intellij.classes.locator.*
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginXmlUtil

/**
 * @author Sergey Patrikeev
 */
class ClassesForCheckSelector {

  private companion object {
    val MAIN_CLASS_LOCATION_KEYS = listOf(JarPluginKey, ClassesDirectoryKey, LibDirectoryKey)

    val ADDITIONAL_CLASS_LOCATION_KEYS = listOf(CompileServerExtensionKey)
  }

  fun getClassesForCheck(plugin: IdePlugin, locationsContainer: ClassLocationsContainer): Iterator<String> {
    val mainUnitedResolver = UnionResolver.create(MAIN_CLASS_LOCATION_KEYS.mapNotNull { locationsContainer.getResolver(it) })
    val referencedResolvers = getAllClassesReferencedFromXml(plugin).mapNotNull { mainUnitedResolver.getClassLocation(it) }

    val additionalResolvers = ADDITIONAL_CLASS_LOCATION_KEYS.mapNotNull { locationsContainer.getResolver(it) }

    val result = UnionResolver.create((referencedResolvers + additionalResolvers).distinct())
    return if (result.isEmpty) locationsContainer.getUnitedResolver().allClasses else result.allClasses
  }

  private fun getAllClassesReferencedFromXml(plugin: IdePlugin): Set<String> =
      PluginXmlUtil.getAllClassesReferencedFromXml(plugin) +
          plugin.optionalDescriptors.flatMap { PluginXmlUtil.getAllClassesReferencedFromXml(it.value) }


}