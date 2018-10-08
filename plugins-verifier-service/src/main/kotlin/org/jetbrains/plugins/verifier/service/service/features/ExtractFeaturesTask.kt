package org.jetbrains.plugins.verifier.service.service.features

import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.intellij.feature.extractor.FeaturesExtractor
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.ide.IdeDescriptorsCache
import com.jetbrains.pluginverifier.ide.IdeRepository
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.misc.pluralizeWithNumber
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.tasks.ProgressIndicator
import org.jetbrains.plugins.verifier.service.tasks.Task
import org.slf4j.LoggerFactory

/**
 * [Task] that runs the [feature extractor] [FeaturesExtractor] for the [updateInfo].
 */
class ExtractFeaturesTask(
    val updateInfo: UpdateInfo,
    private val ideDescriptorsCache: IdeDescriptorsCache,
    private val pluginDetailsCache: PluginDetailsCache,
    private val ideRepository: IdeRepository
) : Task<ExtractFeaturesTask.Result>("Features of $updateInfo", "ExtractFeatures") {

  companion object {
    private val LOG = LoggerFactory.getLogger(ExtractFeaturesTask::class.java)
  }

  private val featureExtractorIdeVersion = IdeVersion.createIdeVersion(Settings.FEATURE_EXTRACTOR_IDE_BUILD.get())

  /**
   * Result of the [feature extractor service] [FeatureExtractorService] task.
   */
  data class Result(
      val updateInfo: UpdateInfo,
      val resultType: ResultType,
      val features: List<ExtensionPointFeatures> = emptyList(),
      val invalidPluginReason: String? = null,
      val notFoundReason: String? = null,
      val failedToDownloadReason: String? = null
  ) {
    enum class ResultType {
      NOT_FOUND,
      FAILED_TO_DOWNLOAD,
      BAD_PLUGIN,
      EXTRACTED_ALL,
      EXTRACTED_PARTIALLY
    }

    override fun toString() = when {
      failedToDownloadReason != null -> "Plugin $updateInfo couldn't be downloaded: $failedToDownloadReason"
      notFoundReason != null -> "Plugin $updateInfo is not found: $notFoundReason"
      invalidPluginReason != null -> "Plugin $updateInfo is invalid: $invalidPluginReason"
      else -> "For plugin $updateInfo there " + "is".pluralize(features.size) + " " + "feature".pluralizeWithNumber(features.size) + " extracted: $resultType"
    }
  }

  override fun execute(progress: ProgressIndicator): Result {
    return getIdeDescriptorForRun().use {
      execute(it.ideDescriptor)
    }
  }

  private fun getIdeDescriptorForRun(): IdeDescriptorsCache.Result.Found {
    val specifiedVersion = ideDescriptorsCache.getIdeDescriptorCacheEntry(featureExtractorIdeVersion)
    if (specifiedVersion is IdeDescriptorsCache.Result.Found) {
      return specifiedVersion
    }
    val maxUltimateVersion = ideRepository.fetchIndex().map { it.version }.filter { it.productCode == "IU" }.max()
    if (maxUltimateVersion != null) {
      LOG.warn("IDE $featureExtractorIdeVersion is not available, defaulting to $maxUltimateVersion")
      val maxUltimateIdeEntry = ideDescriptorsCache.getIdeDescriptorCacheEntry(maxUltimateVersion)
      if (maxUltimateIdeEntry is IdeDescriptorsCache.Result.Found) {
        return maxUltimateIdeEntry
      }
    }
    throw IllegalStateException("IDE $featureExtractorIdeVersion for feature extraction is not available")
  }

  private fun execute(ideDescriptor: IdeDescriptor): Result =
      pluginDetailsCache.getPluginDetailsCacheEntry(updateInfo).use {
        with(it) {
          when (this) {
            is PluginDetailsCache.Result.Provided -> runFeatureExtractor(ideDescriptor, pluginDetails.idePlugin)
            is PluginDetailsCache.Result.FileNotFound -> {
              Result(
                  updateInfo,
                  Result.ResultType.NOT_FOUND,
                  notFoundReason = reason
              )
            }
            is PluginDetailsCache.Result.InvalidPlugin -> {
              Result(
                  updateInfo,
                  Result.ResultType.BAD_PLUGIN,
                  invalidPluginReason = "Plugin configuration is invalid: " + pluginErrors.filter { it.level == PluginProblem.Level.ERROR }.joinToString()
              )
            }
            is PluginDetailsCache.Result.Failed -> {
              LOG.info("Unable to get plugin details for $updateInfo", error)
              Result(
                  updateInfo,
                  Result.ResultType.NOT_FOUND,
                  failedToDownloadReason = reason
              )
            }
          }
        }
      }

  private fun runFeatureExtractor(ideDescriptor: IdeDescriptor, plugin: IdePlugin): Result {
    val extractorResult = FeaturesExtractor.extractFeatures(ideDescriptor.ide, ideDescriptor.ideResolver, plugin)
    val resultType = when {
      extractorResult.extractedAll -> Result.ResultType.EXTRACTED_ALL
      else -> Result.ResultType.EXTRACTED_PARTIALLY
    }
    return Result(updateInfo, resultType, features = extractorResult.features)
  }

}