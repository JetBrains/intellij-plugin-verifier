package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.packages.DefaultPackageFilter
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning

class MockVerificationContext(override val classResolver: Resolver = EmptyResolver) : VerificationContext {
  private val compatibilityProblemRegistrar: SimpleCompatibilityProblemRegistrar = SimpleCompatibilityProblemRegistrar()

  override val externalClassesPackageFilter = DefaultPackageFilter(emptyList())

  override val problemRegistrar = compatibilityProblemRegistrar

  override val warningRegistrar = compatibilityProblemRegistrar

  override val apiUsageProcessors = emptyList<ApiUsageProcessor>()

  val problems: List<CompatibilityProblem>
    get() = compatibilityProblemRegistrar.problems

  val warnings: List<CompatibilityWarning>
    get() = compatibilityProblemRegistrar.warnings
}