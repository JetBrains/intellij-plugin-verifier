package com.jetbrains.intellij.feature.extractor

import com.jetbrains.intellij.feature.extractor.FeaturesExtractor.extractFeatures
import com.jetbrains.intellij.feature.extractor.core.*
import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.classes.locator.ClassLocationsContainer
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesFinder
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import org.objectweb.asm.tree.ClassNode
import org.slf4j.LoggerFactory

/**
 * Main feature extractor entry point.
 *
 * Use [extractFeatures] to extract the plugin features. The class analyses the
 * plugin class-files. Some classes may refer to the platform API constant.
 * This is why the method also takes IDE build (presumably with which the plugin is compatible) as parameter.
 */
object FeaturesExtractor {

  private val LOG = LoggerFactory.getLogger("FeaturesExtractor")

  fun extractFeatures(ide: Ide, ideResolver: Resolver, plugin: IdePlugin): ExtractorResult {
    val bundledClassContainers = createBundledPluginsClassContainers(ide)
    try {
      IdePluginClassesFinder.createLocationsContainer(plugin, emptyList()).use { container ->
        //don't close this resolver, because ideResolver is to be closed by the caller.
        val pluginResolver = container.getUnitedResolver()
        val bundledResolvers = bundledClassContainers.map { it.getUnitedResolver() }
        val resolver = UnionResolver.create(listOf(pluginResolver, ideResolver) + bundledResolvers)
        return implementations(plugin, resolver)
      }
    } finally {
      bundledClassContainers.forEach { it.closeLogged() }
    }
  }

  private fun createBundledPluginsClassContainers(ide: Ide): List<ClassLocationsContainer> {
    val bundledClassContainers = arrayListOf<ClassLocationsContainer>()
    ide.bundledPlugins.mapNotNullTo(bundledClassContainers) {
      try {
        IdePluginClassesFinder.createLocationsContainer(it, emptyList())
      } catch (e: Throwable) {
        LOG.error("Unable to create IDE ($ide) bundled plugin ($it) resolver", e)
        null
      }
    }
    return bundledClassContainers
  }

  private fun implementations(plugin: IdePlugin, resolver: Resolver): ExtractorResult {
    val allEpFeatures = ExtensionPoint.values().map { epFeatures(plugin, it, resolver) }
    return ExtractorResult(allEpFeatures.flatMap { it.features }, allEpFeatures.all { it.extractedAll })
  }

  private fun epFeatures(plugin: IdePlugin, extensionPoint: ExtensionPoint, resolver: Resolver): ExtractorResult {
    val epImplementors = getExtensionPointImplementors(extensionPoint, plugin)
    if (epImplementors.isEmpty()) {
      return ExtractorResult(emptyList(), true)
    }
    val allImplementorsFeatures = epImplementors.map { extractEpFeatures(extensionPoint, it, plugin, resolver) }
    val extractedAll = allImplementorsFeatures.all { it != null && it.second }
    return ExtractorResult(allImplementorsFeatures.filterNotNull().map { it.first }.filterNot { it.featureNames.isEmpty() }, extractedAll)
  }

  private fun extractEpFeatures(extensionPoint: ExtensionPoint, epImplementorClass: String, plugin: IdePlugin, resolver: Resolver): Pair<ExtensionPointFeatures, Boolean>? {
    val classNode: ClassNode
    try {
      classNode = resolver.findClass(epImplementorClass.replace('.', '/')) ?: return null
    } catch (e: Exception) {
      LOG.debug("Unable to get plugin $plugin class file `$epImplementorClass`", e)
      return null
    }

    val extractor = when (extensionPoint) {
      ExtensionPoint.CONFIGURATION_TYPE -> RunConfigurationExtractor(resolver)
      ExtensionPoint.FACET_TYPE -> FacetTypeExtractor(resolver)
      ExtensionPoint.FILE_TYPE -> FileTypeExtractor(resolver)
      ExtensionPoint.ARTIFACT_TYPE -> ArtifactTypeExtractor(resolver)
      ExtensionPoint.MODULE_TYPE -> ModuleTypeExtractor(resolver)
    }
    val result = extractor.extract(classNode)
    if (!result.extractedAll) {
      LOG.info("Not all features extracted from ${classNode.name} ($extensionPoint)")
    }
    return ExtensionPointFeatures(extensionPoint, epImplementorClass, result.featureNames) to result.extractedAll
  }

  private fun getExtensionPointImplementors(extensionPoint: ExtensionPoint, plugin: IdePlugin): List<String> {
    val extensionElements = plugin.extensions[extensionPoint.extensionPointName] ?: return emptyList()
    val result = arrayListOf<String>()
    extensionElements.mapNotNullTo(result) { it.getAttributeValue("implementation") }
    extensionElements.mapNotNullTo(result) { it.getAttributeValue("implementationClass") }
    return result
  }

}