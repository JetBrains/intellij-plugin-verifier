package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import kotlinx.serialization.Serializable
import org.jetbrains.ide.diff.builder.persistence.json.IdeVersionSerializer

/**
 * Container of APIs and associated events built for IDE of version [ideBuildNumber].
 */
@Serializable
data class ApiReport(
  @Serializable(with = IdeVersionSerializer::class) val ideBuildNumber: IdeVersion,
  val apiSignatureToEvents: Map<
    @Serializable(with = ApiSignatureSerializer::class) ApiSignature,
    Set<@Serializable(with = ApiEventSerializer::class) ApiEvent>
    >
) {
  /**
   * Returns this report as a sequence of signatures and corresponding events.
   */
  fun asSequence(): Sequence<Pair<ApiSignature, ApiEvent>> =
    apiSignatureToEvents
      .asSequence()
      .flatMap { (signature, events) -> events.asSequence().map { signature to it } }


  /**
   * Returns all API events associated with the signature.
   */
  operator fun get(apiSignature: ApiSignature): Set<ApiEvent> =
    apiSignatureToEvents.getOrDefault(apiSignature, emptySet())

}