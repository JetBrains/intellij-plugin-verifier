/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.results.location.ElementType
import com.jetbrains.pluginverifier.results.location.toReference
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.results.reference.SymbolicReference
import com.jetbrains.pluginverifier.usages.internal.InternalApiUsage
import java.nio.file.Files
import java.nio.file.Path

private const val INTERNAL_API_USAGES_SUMMARY_SCHEMA_VERSION = 1

/**
 * Machine-readable snapshot of internal API usages observed while verifying plugins against the trunk IDE.
 *
 * [apiElements] is sorted by the number of distinct affected plugin IDs, then by the number of usages.
 * Consumers can take the first 10/100 entries without the producer hard-coding an arbitrary top-N cutoff.
 */
data class InternalApiUsagesSummary(
  val schemaVersion: Int,
  val ideVersion: String,
  val totals: InternalApiUsagesTotals,
  val apiElements: List<InternalApiElementSummary>
) {
  companion object {
    fun from(
      target: PluginVerificationTarget.IDE,
      results: List<PluginVerificationResult>
    ): InternalApiUsagesSummary {
      val accumulators = hashMapOf<SymbolicReference, InternalApiElementAccumulator>()
      val checkedResults = results.filterIsInstance<PluginVerificationResult.Verified>()

      for (result in checkedResults) {
        for (usage in result.internalApiUsages) {
          accumulators.record(result.plugin.pluginId, usage, ignored = false, ignoreReason = null)
        }

        // The current check-trunk-api filters do not populate this map yet. Keep this branch because
        // ignored errors will be supplied by Marketplace data in a follow-up.
        for ((usage, ignoreReason) in result.ignoredInternalApiUsages) {
          accumulators.record(result.plugin.pluginId, usage, ignored = true, ignoreReason = ignoreReason)
        }
      }

      val apiElements = accumulators.values
        .sortedWith(
          compareByDescending<InternalApiElementAccumulator> { it.affectedPluginIds.size }
            .thenByDescending { it.usageCount }
            .thenBy { it.apiElement.stableSortKey() }
        )
        .mapIndexed { index, accumulator -> accumulator.toSummary(index + 1) }

      val affectedPluginIds = hashSetOf<String>()
      val nonIgnoredAffectedPluginIds = hashSetOf<String>()
      val ignoredAffectedPluginIds = hashSetOf<String>()
      var usageCount = 0
      var nonIgnoredUsageCount = 0
      var ignoredUsageCount = 0

      for (accumulator in accumulators.values) {
        affectedPluginIds += accumulator.affectedPluginIds
        nonIgnoredAffectedPluginIds += accumulator.nonIgnoredAffectedPluginIds
        ignoredAffectedPluginIds += accumulator.ignoredAffectedPluginIds
        usageCount += accumulator.usageCount
        nonIgnoredUsageCount += accumulator.nonIgnoredUsageCount
        ignoredUsageCount += accumulator.ignoredUsageCount
      }

      return InternalApiUsagesSummary(
        schemaVersion = INTERNAL_API_USAGES_SUMMARY_SCHEMA_VERSION,
        ideVersion = target.ideVersion.asString(),
        totals = InternalApiUsagesTotals(
          checkedPluginCount = checkedResults.size,
          invalidPluginCount = results.count { it is PluginVerificationResult.InvalidPlugin },
          notFoundPluginCount = results.count { it is PluginVerificationResult.NotFound },
          failedToDownloadPluginCount = results.count { it is PluginVerificationResult.FailedToDownload },
          affectedPluginCount = affectedPluginIds.size,
          nonIgnoredAffectedPluginCount = nonIgnoredAffectedPluginIds.size,
          ignoredAffectedPluginCount = ignoredAffectedPluginIds.size,
          distinctApiElementCount = apiElements.size,
          usageCount = usageCount,
          nonIgnoredUsageCount = nonIgnoredUsageCount,
          ignoredUsageCount = ignoredUsageCount
        ),
        apiElements = apiElements
      )
    }
  }

  fun writeTo(path: Path) {
    path.parent?.let { Files.createDirectories(it) }
    jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValue(path.toFile(), this)
  }
}

data class InternalApiUsagesTotals(
  val checkedPluginCount: Int,
  val invalidPluginCount: Int,
  val notFoundPluginCount: Int,
  val failedToDownloadPluginCount: Int,
  val affectedPluginCount: Int,
  val nonIgnoredAffectedPluginCount: Int,
  val ignoredAffectedPluginCount: Int,
  val distinctApiElementCount: Int,
  val usageCount: Int,
  val nonIgnoredUsageCount: Int,
  val ignoredUsageCount: Int
)

data class InternalApiElementSummary(
  val rank: Int,
  val apiElement: InternalApiElement,
  val affectedPluginCount: Int,
  val nonIgnoredAffectedPluginCount: Int,
  val ignoredAffectedPluginCount: Int,
  val usageCount: Int,
  val nonIgnoredUsageCount: Int,
  val ignoredUsageCount: Int,
  val pluginIds: List<String>,
  val nonIgnoredPluginIds: List<String>,
  val ignoredPluginIds: List<String>,
  val ignoreReasons: List<String>
)

/**
 * Stable machine-oriented identity of an API element.
 *
 * [className], [memberName] and [descriptor] are the fields consumers should use for identity.
 * [presentableLocation] is informational only and may evolve with report presentation code.
 */
data class InternalApiElement(
  val apiElementType: ElementType,
  val className: String,
  val memberName: String?,
  val descriptor: String?,
  val presentableLocation: String
) {
  fun stableSortKey(): String = listOf(apiElementType.name, className, memberName.orEmpty(), descriptor.orEmpty()).joinToString("\u0000")
}

private class InternalApiElementAccumulator(
  val apiElement: InternalApiElement
) {
  var nonIgnoredUsageCount: Int = 0
  var ignoredUsageCount: Int = 0
  val affectedPluginIds = hashSetOf<String>()
  val nonIgnoredAffectedPluginIds = hashSetOf<String>()
  val ignoredAffectedPluginIds = hashSetOf<String>()
  val ignoreReasons = hashSetOf<String>()

  val usageCount: Int
    get() = nonIgnoredUsageCount + ignoredUsageCount

  fun add(pluginId: String, ignored: Boolean, ignoreReason: String?) {
    affectedPluginIds += pluginId
    if (ignored) {
      ignoredUsageCount++
      ignoredAffectedPluginIds += pluginId
      if (!ignoreReason.isNullOrBlank()) {
        ignoreReasons += ignoreReason
      }
    } else {
      nonIgnoredUsageCount++
      nonIgnoredAffectedPluginIds += pluginId
    }
  }

  fun toSummary(rank: Int) = InternalApiElementSummary(
    rank = rank,
    apiElement = apiElement,
    affectedPluginCount = affectedPluginIds.size,
    nonIgnoredAffectedPluginCount = nonIgnoredAffectedPluginIds.size,
    ignoredAffectedPluginCount = ignoredAffectedPluginIds.size,
    usageCount = usageCount,
    nonIgnoredUsageCount = nonIgnoredUsageCount,
    ignoredUsageCount = ignoredUsageCount,
    pluginIds = affectedPluginIds.sorted(),
    nonIgnoredPluginIds = nonIgnoredAffectedPluginIds.sorted(),
    ignoredPluginIds = ignoredAffectedPluginIds.sorted(),
    ignoreReasons = ignoreReasons.sorted()
  )
}

private fun MutableMap<SymbolicReference, InternalApiElementAccumulator>.record(
  pluginId: String,
  usage: InternalApiUsage,
  ignored: Boolean,
  ignoreReason: String?
) {
  // Group by the resolved API element, not by the bytecode reference at the call site. For inherited
  // members those references can differ while still pointing to the same actual internal API.
  val reference = usage.apiElement.toReference()
  getOrPut(reference) {
    InternalApiElementAccumulator(reference.toInternalApiElement(usage))
  }.add(pluginId, ignored, ignoreReason)
}

private fun SymbolicReference.toInternalApiElement(usage: InternalApiUsage): InternalApiElement = when (this) {
  is ClassReference -> InternalApiElement(
    apiElementType = usage.apiElement.elementType,
    className = className,
    memberName = null,
    descriptor = null,
    presentableLocation = usage.apiElement.presentableLocation
  )

  is MethodReference -> InternalApiElement(
    apiElementType = usage.apiElement.elementType,
    className = hostClass.className,
    memberName = methodName,
    descriptor = methodDescriptor,
    presentableLocation = usage.apiElement.presentableLocation
  )

  is FieldReference -> InternalApiElement(
    apiElementType = usage.apiElement.elementType,
    className = hostClass.className,
    memberName = fieldName,
    descriptor = fieldDescriptor,
    presentableLocation = usage.apiElement.presentableLocation
  )
}
