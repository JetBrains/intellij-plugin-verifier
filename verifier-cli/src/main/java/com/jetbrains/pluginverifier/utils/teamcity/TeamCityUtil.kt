package com.jetbrains.pluginverifier.utils.teamcity

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.api.VResult
import com.jetbrains.pluginverifier.api.VResults
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.results.ProblemSet
import java.util.*

//TODO: get rid of it.
object TeamCityUtil {
  fun convertOldResultsToNewResults(results: Map<UpdateInfo, ProblemSet>, ideVersion: IdeVersion): VResults {
    val ideDescriptor = IdeDescriptor.ByVersion(ideVersion)

    val list = arrayListOf<VResult>()
    for (entry in results) {
      val pluginDescriptor = PluginDescriptor.ByUpdateInfo(entry.key)

      val multimap = HashMultimap.create<Problem, ProblemLocation>()
      entry.value.asMap().forEach { entry -> entry.value.forEach { multimap.put(entry.key, it) } }

      if (entry.value.isEmpty) {
        list.add(VResult.Nice(pluginDescriptor, ideDescriptor, ""))
      } else {
        list.add(VResult.Problems(pluginDescriptor, ideDescriptor, "", multimap))
      }
    }

    return VResults(list)
  }

  fun convertToProblemSet(prevBuildProblems: Multimap<Problem, UpdateInfo>): Map<UpdateInfo, ProblemSet> {
    val map = HashMap<UpdateInfo, ProblemSet>()
    prevBuildProblems.entries().forEach { it ->
      map.putIfAbsent(it.value, ProblemSet())
      map[it.value]!!.addProblem(it.key, ProblemLocation.fromPlugin(if (it.value.pluginId != null) it.value.pluginId!! else "#" + it.value.updateId!!))
    }
    return map
  }

}