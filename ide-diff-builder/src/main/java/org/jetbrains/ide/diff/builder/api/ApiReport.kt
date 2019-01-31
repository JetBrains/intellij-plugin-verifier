package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.ide.diff.builder.signatures.ApiSignature

/**
 * Container of APIs and associated events built for IDE of version [ideBuildNumber].
 */
data class ApiReport(val ideBuildNumber: IdeVersion, val apiSignatureToEvents: Map<ApiSignature, Set<ApiEvent>>) {
  /**
   * Returns this report as a sequence of signatures and corresponding events.
   */
  fun asSequence(): Sequence<Pair<ApiSignature, ApiEvent>> =
      apiSignatureToEvents
          .asSequence()
          .flatMap { (signature, events) -> events.asSequence().map { signature to it } }


  /**
   * Returns all API events associated with the signature in this report sorted by IDE version.
   */
  operator fun get(apiSignature: ApiSignature): List<ApiEvent> =
      apiSignatureToEvents.getOrDefault(apiSignature, emptySet()).sortedBy { it.ideVersion }

}