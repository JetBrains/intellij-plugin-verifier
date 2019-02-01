package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.ide.diff.builder.signatures.ApiSignature

/**
 * Merges multiple [ApiReport]s into one, fixing ambiguity in history of [ApiEvent]s, if necessary.
 */
class ApiReportsMerger {

  private fun isBuggySignature(signature: ApiSignature): Boolean {
    val className = signature.externalPresentation.substringBefore(" ")
    return IdeDiffBuilder.hasObfuscatedLikePackage(className)
  }

  fun mergeApiReports(resultIdeVersion: IdeVersion, reports: List<ApiReport>): ApiReport {
    if (reports.isEmpty()) {
      return ApiReport(resultIdeVersion, emptyMap())
    }
    if (reports.size == 1) {
      return reports.single().copy(ideBuildNumber = resultIdeVersion)
    }

    val apiSignatureToEvents = hashMapOf<ApiSignature, MutableSet<ApiEvent>>()
    val processedSignatures = hashSetOf<ApiSignature>()
    for (report in reports) {
      for ((signature, _) in report.asSequence()) {
        if (!isBuggySignature(signature) && processedSignatures.add(signature)) {
          val allEvents = reports.flatMap { it[signature] }
          val mergedEvents = ApiEventsMerger().mergeEvents(allEvents)
          for (event in mergedEvents) {
            apiSignatureToEvents.getOrPut(signature) { hashSetOf() } += event
          }
        }
      }
    }
    return ApiReport(resultIdeVersion, apiSignatureToEvents)
  }

}