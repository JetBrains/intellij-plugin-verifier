package org.jetbrains.plugins.verifier.service.runners

import com.intellij.structure.domain.Plugin
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.intellij.feature.extractor.FeaturesExtractor
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.api.VManager
import com.jetbrains.pluginverifier.api.VResult
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.repository.IFileLock
import org.jetbrains.plugins.verifier.service.core.Progress
import org.jetbrains.plugins.verifier.service.core.Task
import org.jetbrains.plugins.verifier.service.storage.IdeFilesManager
import org.slf4j.LoggerFactory


data class FeaturesResult(val plugin: PluginDescriptor,
                          val resultType: ResultType,
                          val features: List<ExtensionPointFeatures> = emptyList(),
                          val badPlugin: VResult.BadPlugin? = null) {
  enum class ResultType {
    NOT_FOUND,
    BAD_PLUGIN,
    EXTRACTED_ALL,
    EXTRACTED_PARTIALLY
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
    val (plugin: Plugin?, resolver: Resolver?, closeResolver: Boolean, pluginLock: IFileLock?, badResult: VResult?) =
        try {
          VManager.createPluginWithResolver(pluginDescriptor, null)
        } catch(e: Exception) {
          LOG.error("Unable to create plugin for $pluginDescriptor", e)
          throw e
        }

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
        val extractorResult = FeaturesExtractor.extractFeatures(ideLock.ide, plugin, resolver)
        val resultType = if (extractorResult.extractedAll) FeaturesResult.ResultType.EXTRACTED_ALL else FeaturesResult.ResultType.EXTRACTED_PARTIALLY
        return FeaturesResult(pluginDescriptor, resultType, extractorResult.features)
      } catch (e: Exception) {
        LOG.error("Unable to extract features of the plugin: $pluginDescriptor; taskId = $taskId", e)
        throw e
      } finally {
        ideLock.release()
      }

    } finally {
      pluginLock?.release()
      if (closeResolver) {
        resolver?.closeLogged()
      }
    }
  }

}
