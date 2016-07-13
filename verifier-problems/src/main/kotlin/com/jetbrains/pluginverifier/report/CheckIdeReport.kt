package com.jetbrains.pluginverifier.report

import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.jsonDeserializer
import com.github.salomonbrys.kotson.jsonSerializer
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import com.google.gson.annotations.SerializedName
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.persistence.GsonHolder
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.results.GlobalResultsRepository
import java.io.File

/**
 * Creates a Guava multimap using the input map.
 */
fun <K, V> Map<K, Iterable<V>>.multimapFromMap(): Multimap<K, V> {
  val result = ArrayListMultimap.create<K, V>()
  for ((key, values) in this) {
    result.putAll(key, values)
  }
  return result
}


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

  private fun findPreviousReports(ideVersion: IdeVersion): List<CheckIdeReport> {
    val repository = GlobalResultsRepository()
    return repository.availableReportsList
        .map { repository.getReportFile(it) }
        .map { CheckIdeReport.loadFromFile(it) }
        .filter { it.ideVersion.baselineVersion == ideVersion.baselineVersion && it.ideVersion.compareTo(ideVersion) < 0 }
        .sortedBy { it.ideVersion }
  }


  fun compareWithPreviousChecks(): CheckIdeCompareResult {
    val previousReports = findPreviousReports(ideVersion)
    val firstOccurrences: Map<Problem, IdeVersion> = (previousReports + CheckIdeReport(ideVersion, pluginProblems))
        .flatMap { r -> r.pluginProblems.values().map { it to r.ideVersion } }
        .groupBy { it.first }
        .filterValues { it.isNotEmpty() }
        .mapValues { it.value.map { it.second }.min()!! }
    if (previousReports.isEmpty()) {
      return CheckIdeCompareResult(ideVersion, pluginProblems, firstOccurrences)
    }
    val firstProblems = previousReports[0].pluginProblems.values().distinct().toSet()
    val newProblems = pluginProblems.asMap()
        .mapValues { it.value.filterNot { firstProblems.contains(it) } }
        .filterValues { it.isNotEmpty() }
        .multimapFromMap()
    return CheckIdeCompareResult(ideVersion, newProblems, firstOccurrences)
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

data class CheckIdeCompareResult(val checkIdeVersion: IdeVersion,
                                 val pluginProblems: Multimap<UpdateInfo, Problem>,
                                 val firstOccurrences: Map<Problem, IdeVersion>)
