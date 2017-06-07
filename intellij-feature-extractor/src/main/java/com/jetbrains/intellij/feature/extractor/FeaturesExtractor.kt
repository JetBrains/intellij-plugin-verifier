package com.jetbrains.intellij.feature.extractor

import com.intellij.structure.ide.Ide
import com.intellij.structure.plugin.Plugin
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.intellij.feature.extractor.core.ArtifactTypeExtractor
import com.jetbrains.intellij.feature.extractor.core.FacetTypeExtractor
import com.jetbrains.intellij.feature.extractor.core.FileTypeExtractor
import com.jetbrains.intellij.feature.extractor.core.RunConfigurationExtractor
import org.objectweb.asm.tree.ClassNode
import org.slf4j.LoggerFactory

object FeaturesExtractor {

  private val LOG = LoggerFactory.getLogger("FeaturesExtractor")

  fun extractFeatures(ide: Ide, plugin: Plugin): ExtractorResult {

    val bundledResolvers = createBundledPluginResolvers(ide)

    Resolver.createUnionResolver("IDE $ide bundled plugins", bundledResolvers).use { bundledPlugins ->
      (Resolver.createPluginResolver(plugin)).use { pluginResolver ->
        Resolver.createIdeResolver(ide).use { ideResolver ->
          val resolver = Resolver.createUnionResolver("$ide $plugin", listOf(pluginResolver, ideResolver, bundledPlugins))
          return implementations(plugin, resolver)
        }
      }
    }
  }

  private fun createBundledPluginResolvers(ide: Ide): MutableList<Resolver> {
    val bundledResolvers: MutableList<Resolver> = mutableListOf()

    ide.bundledPlugins.forEach {
      try {
        bundledResolvers.add(Resolver.createPluginResolver(it))
      } catch (e: Exception) {
        LOG.error("Unable to create IDE ($ide) bundled plugin ($it) resolver", e)
      }
    }
    return bundledResolvers
  }

  private fun implementations(plugin: Plugin, resolver: Resolver): ExtractorResult {
    val allEpFeatures = ExtensionPoint.values().map { epFeatures(plugin, it, resolver) }
    return ExtractorResult(allEpFeatures.flatMap { it.features }, allEpFeatures.all { it.extractedAll })
  }

  private fun epFeatures(plugin: Plugin, extensionPoint: ExtensionPoint, resolver: Resolver): ExtractorResult {
    val epImplementors = getExtensionPointImplementors(extensionPoint, plugin)
    if (epImplementors.isEmpty()) {
      return ExtractorResult(emptyList(), true)
    }
    val allImplementorsFeatures = epImplementors.map { extractEpFeatures(extensionPoint, it, plugin, resolver) }
    val extractedAll = allImplementorsFeatures.all { it != null && it.second }
    return ExtractorResult(allImplementorsFeatures.filterNotNull().map { it.first }.filterNot { it.featureNames.isEmpty() }, extractedAll)
  }

  private fun extractEpFeatures(extensionPoint: ExtensionPoint, epImplementorClass: String, plugin: Plugin, resolver: Resolver): Pair<ExtensionPointFeatures, Boolean>? {
    val classNode: ClassNode
    try {
      classNode = resolver.findClass(epImplementorClass.replace('.', '/')) ?: return null
    } catch(e: Exception) {
      LOG.debug("Unable to get plugin $plugin class file `$epImplementorClass`", e)
      return null
    }

    val extractor = when (extensionPoint) {
      ExtensionPoint.CONFIGURATION_TYPE -> RunConfigurationExtractor(resolver)
      ExtensionPoint.FACET_TYPE -> FacetTypeExtractor(resolver)
      ExtensionPoint.FILE_TYPE -> FileTypeExtractor(resolver)
      ExtensionPoint.ARTIFACT_TYPE -> ArtifactTypeExtractor(resolver)
    }
    val result = extractor.extract(classNode)
    return ExtensionPointFeatures(extensionPoint, epImplementorClass, result.featureNames) to result.extractedAll
  }

  private fun getExtensionPointImplementors(extensionPoint: ExtensionPoint, plugin: Plugin): List<String> =
      plugin.extensions[extensionPoint.extensionPointName]?.map { it.getAttributeValue("implementation") }?.filterNotNull().orEmpty()

}