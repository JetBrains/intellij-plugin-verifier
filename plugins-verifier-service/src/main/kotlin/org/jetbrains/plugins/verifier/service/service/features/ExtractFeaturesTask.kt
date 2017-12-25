package org.jetbrains.plugins.verifier.service.service.features

import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.intellij.feature.extractor.FeaturesExtractor
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.repository.UpdateInfo
import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.tasks.ProgressIndicator
import org.jetbrains.plugins.verifier.service.tasks.ServiceTask

class ExtractFeaturesTask(val serverContext: ServerContext,
                          val pluginCoordinate: PluginCoordinate,
                          private val updateInfo: UpdateInfo) : ServiceTask<ExtractFeaturesTask.Result>("Features of $pluginCoordinate") {

  /**
   * The result of the feature extractor service [task] [FeatureExtractorService].
   */
  data class Result(val updateInfo: UpdateInfo,
                    val resultType: ResultType,
                    val features: List<ExtensionPointFeatures>) {
    enum class ResultType {
      NOT_FOUND,
      BAD_PLUGIN,
      EXTRACTED_ALL,
      EXTRACTED_PARTIALLY
    }
  }

  override fun execute(progress: ProgressIndicator) =
      getSomeCompatibleIde().use { ideCacheEntryDescriptor ->
        val ideDescriptor = ideCacheEntryDescriptor.resource

        serverContext.pluginDetailsCache.getPluginDetails(updateInfo).use { pluginDetailsCacheEntry ->
          val plugin = pluginDetailsCacheEntry.resource.plugin
              ?: return Result(updateInfo, Result.ResultType.BAD_PLUGIN, emptyList())

          runFeatureExtractor(ideDescriptor, plugin)
        }
      }

  private fun runFeatureExtractor(ideDescriptor: IdeDescriptor,
                                  plugin: IdePlugin): Result {
    val extractorResult = FeaturesExtractor.extractFeatures(ideDescriptor.ide, ideDescriptor.ideResolver, plugin)
    val resultType = when {
      extractorResult.extractedAll -> Result.ResultType.EXTRACTED_ALL
      else -> Result.ResultType.EXTRACTED_PARTIALLY
    }
    return Result(updateInfo, resultType, extractorResult.features)
  }

  private fun getSomeCompatibleIde() = serverContext.ideDescriptorsCache.getIdeDescriptor { availableIdes ->
    availableIdes.find { updateInfo.isCompatibleWith(it) }!!
  }

}