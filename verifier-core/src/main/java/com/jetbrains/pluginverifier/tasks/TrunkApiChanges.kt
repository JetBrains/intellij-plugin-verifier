package com.jetbrains.pluginverifier.tasks

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.Result
import com.jetbrains.pluginverifier.results.Verdict

data class PluginComparingResult(val releaseResult: Result, val trunkResult: Result)

data class TrunkApiChanges(val releaseVersion: IdeVersion,
                           val trunkVersion: IdeVersion,
                           val comparingResults: Map<PluginInfo, PluginComparingResult>) {

  companion object {

    fun create(releaseResult: CheckIdeResult, trunkResult: CheckIdeResult): TrunkApiChanges {
      val trunkPlugin2Result = trunkResult.results.associateBy { it.plugin }
      val releasePlugin2Result = releaseResult.results.associateBy { it.plugin }
      val comparingResults = hashMapOf<PluginInfo, PluginComparingResult>()

      for ((plugin, newResult) in trunkPlugin2Result) {
        val oldResult = releasePlugin2Result[plugin] ?: continue
        val oldNotChecked = oldResult.verdict is Verdict.NotFound || oldResult.verdict is Verdict.FailedToDownload
        val newNotChecked = newResult.verdict is Verdict.NotFound || newResult.verdict is Verdict.FailedToDownload
        if (oldNotChecked || newNotChecked) {
          continue
        }
        comparingResults[plugin] = PluginComparingResult(oldResult, newResult)
      }

      return TrunkApiChanges(releaseResult.ideVersion, trunkResult.ideVersion, comparingResults)
    }

  }
}