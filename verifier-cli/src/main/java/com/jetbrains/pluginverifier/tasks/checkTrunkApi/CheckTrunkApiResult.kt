package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.tasks.TaskResult

class CheckTrunkApiResult(val releaseIdeVersion: IdeVersion,
                          val releaseResults: List<VerificationResult>,
                          val trunkIdeVersion: IdeVersion,
                          val trunkResults: List<VerificationResult>,
                          val comparingResults: Map<PluginInfo, PluginComparingResult>) : TaskResult() {

  companion object {
    fun create(releaseIdeVersion: IdeVersion,
               releaseResults: List<VerificationResult>,
               trunkIdeVersion: IdeVersion,
               trunkResults: List<VerificationResult>): CheckTrunkApiResult {
      val trunkPlugin2Result = trunkResults.associateBy { it.plugin }
      val releasePlugin2Result = releaseResults.associateBy { it.plugin }
      val comparingResults = hashMapOf<PluginInfo, PluginComparingResult>()

      for ((plugin, newResult) in trunkPlugin2Result) {
        val oldResult = releasePlugin2Result[plugin] ?: continue
        val oldNotChecked = oldResult is VerificationResult.NotFound || oldResult is VerificationResult.FailedToDownload
        val newNotChecked = newResult is VerificationResult.NotFound || newResult is VerificationResult.FailedToDownload
        if (oldNotChecked || newNotChecked) {
          continue
        }
        comparingResults[plugin] = PluginComparingResult(plugin, oldResult, newResult)
      }

      return CheckTrunkApiResult(releaseIdeVersion, releaseResults, trunkIdeVersion, trunkResults, comparingResults)
    }

  }


}
