package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.Result
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.tasks.TaskResult

data class CheckTrunkApiResult(val releaseIdeVersion: IdeVersion,
                               val releaseResults: List<Result>,
                               val trunkIdeVersion: IdeVersion,
                               val trunkResults: List<Result>,
                               val comparingResults: Map<PluginInfo, PluginComparingResult>) : TaskResult {

  companion object {
    fun create(releaseIdeVersion: IdeVersion,
               releaseResults: List<Result>,
               trunkIdeVersion: IdeVersion,
               trunkResults: List<Result>): CheckTrunkApiResult {
      val trunkPlugin2Result = trunkResults.associateBy { it.plugin }
      val releasePlugin2Result = releaseResults.associateBy { it.plugin }
      val comparingResults = hashMapOf<PluginInfo, PluginComparingResult>()

      for ((plugin, newResult) in trunkPlugin2Result) {
        val oldResult = releasePlugin2Result[plugin] ?: continue
        val oldNotChecked = oldResult.verdict is Verdict.NotFound || oldResult.verdict is Verdict.FailedToDownload
        val newNotChecked = newResult.verdict is Verdict.NotFound || newResult.verdict is Verdict.FailedToDownload
        if (oldNotChecked || newNotChecked) {
          continue
        }
        comparingResults[plugin] = PluginComparingResult(plugin, oldResult, newResult)
      }

      return CheckTrunkApiResult(releaseIdeVersion, releaseResults, trunkIdeVersion, trunkResults, comparingResults)
    }

  }


}
