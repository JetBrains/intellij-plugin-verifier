package org.jetbrains.plugins.verifier.service.runners

import com.google.gson.annotations.SerializedName
import com.intellij.structure.domain.Plugin
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.intellij.feature.extractor.FeatureImplementation
import com.jetbrains.intellij.feature.extractor.FeaturesExtractor
import com.jetbrains.pluginverifier.api.CreatePluginResult
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.api.VManager
import com.jetbrains.pluginverifier.api.VResult
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.repository.IFileLock
import org.jetbrains.plugins.verifier.service.core.Progress
import org.jetbrains.plugins.verifier.service.core.Task
import org.jetbrains.plugins.verifier.service.storage.IdeFilesManager
import org.slf4j.LoggerFactory


data class FeaturesResult(@SerializedName("plugin") val plugin: PluginDescriptor,
                          @SerializedName("resultType") val resultType: ResultType,
                          @SerializedName("features") val features: List<FeatureImplementation> = emptyList(),
                          @SerializedName("badPlugin") val badPlugin: VResult.BadPlugin? = null) {
  enum class ResultType {
    NOT_FOUND,
    BAD_PLUGIN,
    EXTRACTED
  }
}

/**
 * @author Sergey Patrikeev
 */
class ExtractFeaturesRunner(val pluginDescriptor: PluginDescriptor) : Task<FeaturesResult>() {
  override fun presentableName(): String = "ExtractFeatures of $pluginDescriptor"

  companion object {
    private val LOG = LoggerFactory.getLogger(CheckRangeRunner::class.java)
  }

  override fun computeResult(progress: Progress): FeaturesResult {
    val createResult: CreatePluginResult
    try {
      createResult = VManager.createPluginWithResolver(pluginDescriptor, null)
    } catch(e: Exception) {
      LOG.error("Unable to create plugin for $pluginDescriptor", e)
      throw e
    }
    val (plugin: Plugin?, resolver: Resolver?, pluginLock: IFileLock?, badResult: VResult?) = createResult

    if (badResult != null) {
      return when (badResult) {
        is VResult.NotFound -> FeaturesResult(pluginDescriptor, FeaturesResult.ResultType.NOT_FOUND)
        is VResult.BadPlugin -> FeaturesResult(pluginDescriptor, FeaturesResult.ResultType.BAD_PLUGIN, badPlugin = badResult)
        else -> throw IllegalStateException()
      }
    }

    try {
      val sinceBuild = plugin!!.sinceBuild

      if (sinceBuild == null) {
        val reason = "The plugin $plugin has not specified the <idea-version since-build=\"\"/> attribute"
        return FeaturesResult(pluginDescriptor, FeaturesResult.ResultType.BAD_PLUGIN, badPlugin = VResult.BadPlugin(pluginDescriptor, reason))
      }

      val untilBuild = plugin.untilBuild

      val ideLock: IdeFilesManager.IdeLock = IdeFilesManager.locked {
        val maxCompatible = IdeFilesManager.ideList()
            .filter { sinceBuild <= it && (untilBuild == null || it <= untilBuild) }
            .max()
        if (maxCompatible != null) {
          IdeFilesManager.getIde(maxCompatible)!!
        } else {
          val max = IdeFilesManager.ideList().max()!!
          IdeFilesManager.getIde(max)!!
        }
      }

      try {
        val features = FeaturesExtractor.extractFeatures(ideLock.ide, plugin, resolver)
        return FeaturesResult(pluginDescriptor, FeaturesResult.ResultType.EXTRACTED, features)
      } catch (e: Exception) {
        LOG.error("Unable to extract features of the plugin: $pluginDescriptor; taskId = $taskId", e)
        throw e
      } finally {
        ideLock.release()
      }

    } finally {
      pluginLock?.release()
      resolver?.closeLogged()
    }
  }

}
