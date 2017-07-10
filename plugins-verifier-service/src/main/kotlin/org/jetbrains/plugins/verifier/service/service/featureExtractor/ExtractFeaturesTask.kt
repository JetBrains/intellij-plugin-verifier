package org.jetbrains.plugins.verifier.service.service.featureExtractor

import com.intellij.structure.ide.IdeVersion
import com.jetbrains.intellij.feature.extractor.FeaturesExtractor
import com.jetbrains.pluginverifier.api.PluginCoordinate
import com.jetbrains.pluginverifier.api.PluginInfo
import com.jetbrains.pluginverifier.ide.IdeCreator
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.plugin.PluginCreator
import org.jetbrains.plugins.verifier.service.ide.IdeFileLock
import org.jetbrains.plugins.verifier.service.ide.IdeFilesManager
import org.jetbrains.plugins.verifier.service.progress.TaskProgress
import org.jetbrains.plugins.verifier.service.tasks.Task

class ExtractFeaturesTask(val pluginCoordinate: PluginCoordinate, val pluginInfo: PluginInfo) : Task<FeaturesResult>() {
  override fun presentableName(): String = "Features of $pluginCoordinate"

  override fun computeResult(progress: TaskProgress): FeaturesResult {
    val createPluginResult = PluginCreator.createPlugin(pluginCoordinate)
    createPluginResult.use {
      return when (createPluginResult) {
        is CreatePluginResult.OK -> doFeatureExtraction(createPluginResult)
        is CreatePluginResult.BadPlugin -> FeaturesResult(pluginInfo, FeaturesResult.ResultType.BAD_PLUGIN, emptyList())
        is CreatePluginResult.NotFound -> FeaturesResult(pluginInfo, FeaturesResult.ResultType.NOT_FOUND, emptyList())
      }
    }
  }

  private fun doFeatureExtraction(createPluginResult: CreatePluginResult.OK): FeaturesResult {
    val plugin = createPluginResult.plugin

    val sinceBuild = plugin.sinceBuild!!
    val untilBuild = plugin.untilBuild

    getSomeIdeMatchingSinceUntilBuilds(sinceBuild, untilBuild).use { ideFileLock ->
      val ideDescriptor = IdeCreator.createByFile(ideFileLock.ideFile, null)
      val extractorResult = FeaturesExtractor.extractFeatures(ideDescriptor.ide, plugin)
      val resultType = if (extractorResult.extractedAll) FeaturesResult.ResultType.EXTRACTED_ALL else FeaturesResult.ResultType.EXTRACTED_PARTIALLY
      return FeaturesResult(pluginInfo, resultType, extractorResult.features)
    }
  }

  private fun getSomeIdeMatchingSinceUntilBuilds(sinceBuild: IdeVersion, untilBuild: IdeVersion?): IdeFileLock = IdeFilesManager.lockAndAccess {
    val isMatching: (IdeVersion) -> Boolean = { sinceBuild <= it && (untilBuild == null || it <= untilBuild) }
    val maxCompatibleOrGlobalCompatible = IdeFilesManager.ideList().filter(isMatching).max() ?: IdeFilesManager.ideList().max()!!
    IdeFilesManager.getIdeLock(maxCompatibleOrGlobalCompatible)!!
  }
}
