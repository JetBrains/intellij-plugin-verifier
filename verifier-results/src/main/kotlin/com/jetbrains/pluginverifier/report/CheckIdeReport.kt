package com.jetbrains.pluginverifier.report

import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.jsonDeserializer
import com.github.salomonbrys.kotson.jsonSerializer
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import com.google.gson.annotations.SerializedName
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.api.VResult
import com.jetbrains.pluginverifier.api.VResults
import com.jetbrains.pluginverifier.dependencies.MissingPlugin
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.persistence.GsonHolder
import com.jetbrains.pluginverifier.persistence.multimapFromMap
import com.jetbrains.pluginverifier.problems.Problem
import java.io.File


private data class CheckIdeReportCompact(@SerializedName("ideVersion") val ideVersion: IdeVersion,
                                         @SerializedName("pluginProblems") val pluginProblems: Multimap<UpdateInfo, Int>,
                                         @SerializedName("problems") val problems: List<Problem>,
                                         @SerializedName("missing") val missingPlugins: Multimap<MissingPlugin, UpdateInfo>)

internal val checkIdeReportSerializer = jsonSerializer<CheckIdeReport> {
  val problemToId = it.src.pluginProblems.values().distinct().mapIndexed { i, problem -> problem to i }.associateBy({ it.first }, { it.second })
  val updateToProblemIds: Multimap<UpdateInfo, Int> = it.src.pluginProblems.asMap().mapValues { it.value.map { problemToId[it]!! } }.multimapFromMap()

  it.context.serialize(
      CheckIdeReportCompact(it.src.ideVersion, updateToProblemIds, problemToId.keys.toList(), it.src.missingPlugins)
  )
}

internal val checkIdeReportDeserializer = jsonDeserializer<CheckIdeReport> {
  val compact = it.context.deserialize<CheckIdeReportCompact>(it.json)
  CheckIdeReport(
      compact.ideVersion,
      compact.pluginProblems.asMap().mapValues { it.value.map { compact.problems[it] } }.multimapFromMap(),
      compact.missingPlugins
  )
}

/**
 * @author Sergey Patrikeev
 */
data class CheckIdeReport(@SerializedName("ideVersion") val ideVersion: IdeVersion,
                          @SerializedName("pluginProblems") val pluginProblems: Multimap<UpdateInfo, Problem>,
                          @SerializedName("missingPlugins") val missingPlugins: Multimap<MissingPlugin, UpdateInfo>) {
  fun saveToFile(file: File) {
    file.writeText(GsonHolder.GSON.toJson(this))
  }

  companion object {

    fun createReport(ideVersion: IdeVersion, results: VResults): CheckIdeReport {
      val pluginResults: Map<UpdateInfo, VResult.Problems> = results.results
          .filter { ideVersion == it.ideDescriptor.ideVersion }
          .filterIsInstance(VResult.Problems::class.java)
          .filter { it.pluginDescriptor is PluginDescriptor.ByUpdateInfo }
          .associateBy({ (it.pluginDescriptor as PluginDescriptor.ByUpdateInfo).updateInfo })

      val pluginToMissing = pluginResults.mapValues { it.value.dependenciesGraph.vertices.flatMap { it.missingDependencies.keys.map { MissingPlugin(it.id) } }.distinct() }.multimapFromMap()
      val missingPlugins: Multimap<MissingPlugin, UpdateInfo> = Multimaps.invertFrom(pluginToMissing, ArrayListMultimap.create())

      val pluginProblems = pluginResults.mapValues { it.value.problems.keySet() }.multimapFromMap()
      return CheckIdeReport(ideVersion, pluginProblems, missingPlugins)
    }

    fun loadFromFile(file: File): CheckIdeReport {
      val lines = file.readLines()
      if (lines.isEmpty()) {
        throw IllegalArgumentException("The supplied file doesn't contain check ide report")
      }
      val s = lines.reduce { s, t -> s + t }
      return GsonHolder.GSON.fromJson(s)
    }
  }
}