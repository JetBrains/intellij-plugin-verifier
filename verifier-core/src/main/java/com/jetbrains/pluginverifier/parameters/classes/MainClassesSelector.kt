package com.jetbrains.pluginverifier.parameters.classes

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesFinder
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginXmlUtil

/**
 * [ClassesSelector] that selects for verification
 * all the classes that constitute the
 * plugin's class loader in IDEA, except for the
 * bundled libraries' classes.
 *
 * Specifically, those classes are
 * 1) all classes referenced in the `plugin.xml` and all adjucent ones;
 * 1) for .jar-red plugins, all the classes contained in `.jar`;
 * 2) for .zip-ped plugins, all the classes from the `/lib` directory
 * and from the `/classes` directory.
 *
 * Note that this [selector] [MainClassesSelector] tries
 * to ignore third-party classes from libraries bundled into the plugin distribution
 * because a) they typically don't contain IntelliJ API usages, and b)
 * the verification may produce false warnings as some libraries
 * optionally depend on another libraries, which may not be resolved.
 */
class MainClassesSelector : ClassesSelector {

  /**
   * Selects the plugin's classes that can be referenced by the plugin and its dependencies.
   */
  override fun getClassLoader(classesLocations: IdePluginClassesLocations): Resolver = UnionResolver.create(
      IdePluginClassesFinder.MAIN_CLASSES_KEYS.mapNotNull { classesLocations.getResolver(it) }
  )

  /**
   * Determines plugin's classes that must be verified.
   *
   * The purpose of this method is to filter out the unrelated classes which
   * don't use the IntelliJ API: many plugins pack the libraries without all transitive dependencies.
   * We don't want to check such classes because it leads to false warnings.
   *
   * Instead, our approach is to look at classes referenced in the `plugin.xml`. Given these classes
   * we predict the .jar-files that correspond to the plugin itself (not the secondary bundled libraries).
   */
  override fun getClassesForCheck(classesLocations: IdePluginClassesLocations): Set<String> {
    val mainResolvers = IdePluginClassesFinder.MAIN_CLASSES_KEYS
        .mapNotNull { classesLocations.getResolver(it) }
        .flatMap { it.finalResolvers }

    val mainUnitedResolver = UnionResolver.create(mainResolvers)
    val referencedResolvers = getAllClassesReferencedFromXml(classesLocations.idePlugin)
        .mapNotNull { mainUnitedResolver.getClassLocation(it) }
        .distinct()
        .let { if (it.isEmpty()) mainResolvers else it }

    return UnionResolver.create(referencedResolvers).allClasses
  }

  private fun getAllClassesReferencedFromXml(plugin: IdePlugin): Set<String> =
      PluginXmlUtil.getAllClassesReferencedFromXml(plugin) +
          plugin.optionalDescriptors.flatMap { PluginXmlUtil.getAllClassesReferencedFromXml(it.value) }


}