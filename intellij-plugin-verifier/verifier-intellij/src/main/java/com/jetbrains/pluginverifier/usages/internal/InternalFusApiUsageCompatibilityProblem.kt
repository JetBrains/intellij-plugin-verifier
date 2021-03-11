package com.jetbrains.pluginverifier.usages.internal

import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import java.util.*

class InternalFusApiUsageCompatibilityProblem(
  private val internalApiUsage: InternalApiUsage
) : CompatibilityProblem() {

  private companion object {
    const val DESCRIPTION = "Usage of FUS internal API"
  }

  override val problemType
    get() = DESCRIPTION

  override val shortDescription
    get() = "$DESCRIPTION: ${internalApiUsage.shortDescription.decapitalize()}"

  override val fullDescription
    get() = "$DESCRIPTION: ${internalApiUsage.fullDescription.decapitalize()}"

  override fun equals(other: Any?) = other is InternalFusApiUsageCompatibilityProblem
    && internalApiUsage == other.internalApiUsage

  override fun hashCode() = Objects.hash(internalApiUsage)
}