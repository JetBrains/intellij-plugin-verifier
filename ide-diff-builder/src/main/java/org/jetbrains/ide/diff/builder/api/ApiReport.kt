package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.ide.diff.builder.signatures.ApiSignature

/**
 * Container of APIs and associated events built for IDE of version [ideBuildNumber].
 */
data class ApiReport(val ideBuildNumber: IdeVersion, val apiEventToData: Map<ApiEvent, ApiData>) {
  /**
   * Returns this report as a sequence of signatures and corresponding events.
   */
  fun asSequence(): Sequence<Pair<ApiSignature, ApiEvent>> =
      apiEventToData
          .asSequence()
          .flatMap { (apiEvent, signatures) ->
            signatures.apiSignatures.asSequence().map { it to apiEvent }
          }

  /**
   * Returns all API events associated with the signature in this report.
   */
  operator fun get(apiSignature: ApiSignature): List<ApiEvent> =
      apiEventToData.filter { it.value.contains(apiSignature) }.map { it.key }

}