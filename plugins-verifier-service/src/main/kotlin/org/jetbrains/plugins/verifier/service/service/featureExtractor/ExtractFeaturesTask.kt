package org.jetbrains.plugins.verifier.service.service.featureExtractor

import com.jetbrains.intellij.feature.extractor.FeaturesExtractor
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.IdeCreator
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.repository.UpdateInfo
import org.jetbrains.plugins.verifier.service.ide.IdeFileLock
import org.jetbrains.plugins.verifier.service.ide.IdeFilesManager
import org.jetbrains.plugins.verifier.service.service.ServerInstance
import org.jetbrains.plugins.verifier.service.tasks.Task
import org.jetbrains.plugins.verifier.service.tasks.TaskProgress

class ExtractFeaturesTask(val pluginCoordinate: PluginCoordinate,
                          private val updateInfo: UpdateInfo) : Task<FeaturesResult>() {
  override fun presentableName(): String = "Features of $pluginCoordinate"

  override fun computeResult(progress: TaskProgress): FeaturesResult {
    val pluginDetails = ServerInstance.pluginDetailsProvider.fetchPluginDetails(pluginCoordinate)
    pluginDetails.use {
      return when (pluginDetails) {
        is PluginDetails.ByFileLock -> doFeatureExtraction(pluginDetails.plugin)
        is PluginDetails.FoundOpenPluginAndClasses -> doFeatureExtraction(pluginDetails.plugin)
        is PluginDetails.BadPlugin -> FeaturesResult(updateInfo, FeaturesResult.ResultType.BAD_PLUGIN, emptyList())
        is PluginDetails.NotFound -> FeaturesResult(updateInfo, FeaturesResult.ResultType.NOT_FOUND, emptyList())
        is PluginDetails.FailedToDownload -> FeaturesResult(updateInfo, FeaturesResult.ResultType.NOT_FOUND, emptyList())
        is PluginDetails.FoundOpenPluginWithoutClasses -> FeaturesResult(updateInfo, FeaturesResult.ResultType.EXTRACTED_ALL, emptyList())
      }
    }
  }

  private fun doFeatureExtraction(plugin: IdePlugin): FeaturesResult {

    val sinceBuild = plugin.sinceBuild!!
    val untilBuild = plugin.untilBuild

    getSomeIdeMatchingSinceUntilBuilds(sinceBuild, untilBuild).use { ideFileLock ->
      IdeCreator.createByFile(ideFileLock.ideFile, null).use { (ide, ideResolver) ->
        val extractorResult = FeaturesExtractor.extractFeatures(ide, ideResolver, plugin)
        val resultType = if (extractorResult.extractedAll) FeaturesResult.ResultType.EXTRACTED_ALL else FeaturesResult.ResultType.EXTRACTED_PARTIALLY
        return FeaturesResult(updateInfo, resultType, extractorResult.features)
      }
    }
  }

  private fun getSomeIdeMatchingSinceUntilBuilds(sinceBuild: IdeVersion, untilBuild: IdeVersion?): IdeFileLock = IdeFilesManager.lockAndAccess {
    val isMatching: (IdeVersion) -> Boolean = { sinceBuild <= it && (untilBuild == null || it <= untilBuild) }
    val maxCompatibleOrGlobalCompatible = IdeFilesManager.ideList().filter(isMatching).max() ?: IdeFilesManager.ideList().max()!!
    IdeFilesManager.getIdeLock(maxCompatibleOrGlobalCompatible)!!
  }
}
