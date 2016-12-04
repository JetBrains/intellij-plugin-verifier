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
  FeaturesExtractor.extractFeatures(ide, plugin).forEach { println(GsonHolder.GSON.toJson(it)) }
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

  fun extractFeatures(ide: Ide, plugin: Plugin, createdPluginResolver: Resolver? = null): List<FeatureImplementation> {

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

  private fun implementations(plugin: Plugin, resolver: Resolver) = Feature.values().flatMap { implementations(plugin, it, resolver) }

  private fun implementations(plugin: Plugin, feature: Feature, resolver: Resolver): List<FeatureImplementation> {
    val implementors = pluginImplementors(feature, plugin)
    if (implementors.isEmpty()) {
      return emptyList()
    }
    return implementors.map { implementation(it, plugin, feature, resolver) }.filterNotNull().filterNot { it.featureNames.isEmpty() }
  }

  private fun implementation(implementor: String, plugin: Plugin, feature: Feature, resolver: Resolver): FeatureImplementation? {
    val classNode: ClassNode
    try {
      classNode = resolver.findClass(implementor.replace('.', '/')) ?: return null
    } catch(e: Exception) {
      LOG.error("Unable to get plugin $plugin class file `$implementor`", e)
      return null
    }

    val extractor = when (feature) {
      Feature.ConfigurationType -> RunConfigurationExtractor(resolver)
      Feature.FacetType -> FacetTypeExtractor(resolver)
      Feature.FileType -> FileTypeExtractor(resolver)
      Feature.ArtifactType -> ArtifactTypeExtractor(resolver)
    }
    val featureNames = extractor.extract(classNode) ?: return null

    return FeatureImplementation(feature, implementor, featureNames)
  }

  private fun pluginImplementors(feature: Feature, plugin: Plugin): List<String> =
      plugin.extensions[feature.type]?.map { it.getAttributeValue("implementation") }?.filterNotNull() ?: emptyList()

}