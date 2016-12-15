package com.jetbrains.intellij.feature.extractor

import com.google.gson.annotations.SerializedName
import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.IdeManager
import com.intellij.structure.domain.Plugin
import com.intellij.structure.domain.PluginManager
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.persistence.GsonHolder
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.slf4j.LoggerFactory
import java.io.File

/**
 * @author Sergey Patrikeev
 */
fun main(args: Array<String>) {
  if (args.size != 2) {
    throw IllegalArgumentException("Usage: <plugin> <idea>")
  }
  val pluginFile = File(args[0])
  val ideaFile = File(args[1])
  val plugin = PluginManager.getInstance().createPlugin(pluginFile, false)
  val ide = IdeManager.getInstance().createIde(ideaFile)
  val extractorResult = FeaturesExtractor.extractFeatures(ide, plugin)
  extractorResult.features.forEach { println(GsonHolder.GSON.toJson(it)) }
  println("All features extracted: ${extractorResult.extractedAll}")
}

data class FeatureImplementation(@SerializedName("feature") val feature: Feature,
                                 @SerializedName("implementor") val implementor: String,
                                 @SerializedName("featureNames") val featureNames: List<String>)


enum class Feature(@SerializedName("type") val type: String) {
  ConfigurationType("com.intellij.configurationType"),
  FacetType("com.intellij.facetType"),
  FileType("com.intellij.fileTypeFactory"),
  ArtifactType("com.intellij.packaging.artifactType")
  //TODO: module type: see https://plugins.jetbrains.com/plugin/9238 for example
}

object FeaturesExtractor {

  private val LOG = LoggerFactory.getLogger("FeaturesExtractor")

  data class ExtractorResult(val features: List<FeatureImplementation>, val extractedAll: Boolean)

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
      plugin.extensions[feature.type]?.map { it.getAttributeValue("implementation") }?.filterNotNull() ?: emptyList()

}