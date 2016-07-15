package com.jetbrains.pluginverifier.report

import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.jsonDeserializer
import com.github.salomonbrys.kotson.jsonSerializer
import com.google.common.collect.Multimap
import com.google.gson.annotations.SerializedName
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.persistence.GsonHolder
import com.jetbrains.pluginverifier.persistence.multimapFromMap
import com.jetbrains.pluginverifier.problems.Problem
import java.io.File


private data class CheckIdeReportCompact(@SerializedName("ideVersion") val ideVersion: IdeVersion,
                                         @SerializedName("pluginProblems") val pluginProblems: Multimap<UpdateInfo, Int>,
                                         @SerializedName("problems") val problems: List<Problem>)

val checkIdeReportSerializer = jsonSerializer<CheckIdeReport> {
  val problemToId = it.src.pluginProblems.values().distinct().mapIndexed { i, problem -> problem to i }.associateBy({ it.first }, { it.second })
  val updateToProblemIds: Multimap<UpdateInfo, Int> = it.src.pluginProblems.asMap().mapValues { it.value.map { problemToId[it]!! } }.multimapFromMap()

  it.context.serialize(
      CheckIdeReportCompact(it.src.ideVersion, updateToProblemIds, problemToId.keys.toList())
  )
}

val checkIdeReportDeserializer = jsonDeserializer<CheckIdeReport> {
  val compact = it.context.deserialize<CheckIdeReportCompact>(it.json)
  CheckIdeReport(
      compact.ideVersion,
      compact.pluginProblems.asMap().mapValues { it.value.map { compact.problems[it] } }.multimapFromMap()
  )
}


/**
 * @author Sergey Patrikeev
 */
data class CheckIdeReport(@SerializedName("ideVersion") val ideVersion: IdeVersion,
                          @SerializedName("pluginProblems") val pluginProblems: Multimap<UpdateInfo, Problem>) {
  fun saveToFile(file: File) {
    file.writeText(GsonHolder.GSON.toJson(this))
  }

  companion object {
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

