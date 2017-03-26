package com.jetbrains.intellij.feature.extractor

import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.Plugin
import com.intellij.structure.resolvers.Resolver
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.slf4j.LoggerFactory

object FeaturesExtractor {

  private val LOG = LoggerFactory.getLogger("FeaturesExtractor")

  fun extractFeatures(ide: Ide, plugin: Plugin, createdPluginResolver: Resolver? = null): ExtractorResult {

    val bundledResolvers = createBundledPluginResolvers(ide)

    Resolver.createUnionResolver("IDE $ide bundled plugins", bundledResolvers).use { bundledPlugins ->
      (createdPluginResolver ?: Resolver.createPluginResolver(plugin)).use { pluginResolver ->
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
    val results = Feature.values().map { implementations(plugin, it, resolver) }
    return ExtractorResult(results.flatMap { it.features }, results.all { it.extractedAll == true })
  }

  private fun implementations(plugin: Plugin, feature: Feature, resolver: Resolver): ExtractorResult {
    val implementors = pluginImplementors(feature, plugin)
    if (implementors.isEmpty()) {
      return ExtractorResult(emptyList(), true)
    }
    val implementations = implementors.map { implementation(it, plugin, feature, resolver) }
    val extractedAll = implementations.all { it != null && it.second }
    return ExtractorResult(implementations.filterNotNull().map { it.first }.filterNot { it.featureNames.isEmpty() }, extractedAll)
  }

  private fun implementation(implementor: String, plugin: Plugin, feature: Feature, resolver: Resolver): Pair<FeatureImplementation, Boolean>? {
    val classNode: ClassNode
    try {
      classNode = resolver.findClass(implementor.replace('.', '/')) ?: return null
    } catch(e: Exception) {
      LOG.debug("Unable to get plugin $plugin class file `$implementor`", e)
      return null
    }

    val extractor = when (feature) {
      Feature.ConfigurationType -> RunConfigurationExtractor(resolver)
      Feature.FacetType -> FacetTypeExtractor(resolver)
      Feature.FileType -> FileTypeExtractor(resolver)
      Feature.ArtifactType -> ArtifactTypeExtractor(resolver)
    }
    val result = extractor.extract(classNode)
    return FeatureImplementation(feature, implementor, result.features) to result.extractedAll
  }

  private fun pluginImplementors(feature: Feature, plugin: Plugin): List<String> =
      plugin.extensions[feature.extensionPointName]?.map { it.getAttributeValue("implementation") }?.filterNotNull().orEmpty()

}