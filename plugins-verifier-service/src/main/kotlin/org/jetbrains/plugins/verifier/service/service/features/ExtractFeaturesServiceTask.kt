package org.jetbrains.plugins.verifier.service.service.features

import com.jetbrains.intellij.feature.extractor.FeaturesExtractor
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.parameters.ide.IdeCreator
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.repository.files.FileLock
import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.service.tasks.ServiceTask
import org.jetbrains.plugins.verifier.service.service.tasks.ServiceTaskProgress

class ExtractFeaturesServiceTask(val pluginCoordinate: PluginCoordinate,
                                 private val updateInfo: UpdateInfo,
                                 serverContext: ServerContext) : ServiceTask(serverContext) {
  override fun presentableName(): String = "Features of $pluginCoordinate"

  override fun computeResult(progress: ServiceTaskProgress): FeaturesResult {
    val pluginDetails = serverContext.pluginDetailsProvider.providePluginDetails(pluginCoordinate)
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

    val ideFileLock = getSomeIdeMatchingSinceUntilBuilds(sinceBuild, untilBuild)
    ideFileLock.use {
      IdeCreator.createByFile(ideFileLock.file, null).use { (ide, ideResolver) ->
        val extractorResult = FeaturesExtractor.extractFeatures(ide, ideResolver, plugin)
        val resultType = if (extractorResult.extractedAll) FeaturesResult.ResultType.EXTRACTED_ALL else FeaturesResult.ResultType.EXTRACTED_PARTIALLY
        return FeaturesResult(updateInfo, resultType, extractorResult.features)
      }
    }
  }

  private fun getSomeIdeMatchingSinceUntilBuilds(sinceBuild: IdeVersion, untilBuild: IdeVersion?): FileLock = with(serverContext.ideFilesBank) {
    lockAndAccess {
      val isMatching: (IdeVersion) -> Boolean = { sinceBuild <= it && (untilBuild == null || it <= untilBuild) }
      val maxCompatibleOrGlobalCompatible = getAvailableIdeVersions().filter(isMatching).max() ?: getAvailableIdeVersions().max()!!
      getIdeLock(maxCompatibleOrGlobalCompatible)!!
    }
  }


}
