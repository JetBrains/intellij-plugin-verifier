package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.ide.diff.builder.signatures.ApiSignature

/**
 * Merges multiple [ApiReport]s into one, fixing ambiguity in history of [ApiEvent]s, if necessary.
 */
class ApiReportsMerger {

  fun mergeApiReports(resultIdeVersion: IdeVersion, reports: List<ApiReport>): ApiReport {
    if (reports.isEmpty()) {
      return ApiReport(resultIdeVersion, emptyMap())
    }
    if (reports.size == 1) {
      return reports.single().copy(ideBuildNumber = resultIdeVersion)
    }

    val apiEventToData = hashMapOf<ApiEvent, MutableSet<ApiSignature>>()
    val processedSignatures = hashSetOf<ApiSignature>()
    for (report in reports) {
      for ((signature, _) in report.asSequence()) {
        if (processedSignatures.add(signature)) {
          val allEvents = reports.flatMap { it[signature] }
          val mergedEvents = ApiEventsMerger().mergeEvents(allEvents)
          for (event in mergedEvents) {
            apiEventToData.getOrPut(event) { hashSetOf() } += signature
          }
        }
      }
    }
    return ApiReport(resultIdeVersion, apiEventToData.mapValues { ApiData(it.value) })
  }

}