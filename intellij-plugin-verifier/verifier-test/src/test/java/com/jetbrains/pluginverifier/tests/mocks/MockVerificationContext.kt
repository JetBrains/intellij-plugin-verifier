package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.ProblemRegistrar
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.packages.DefaultPackageFilter
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning
import com.jetbrains.pluginverifier.warnings.WarningRegistrar

class MockVerificationContext : VerificationContext {
  override val classResolver = EmptyResolver

  override val externalClassesPackageFilter = DefaultPackageFilter(emptyList())

  override val problemRegistrar = object : ProblemRegistrar {
    override fun registerProblem(problem: CompatibilityProblem) {
      // NO-OP
    }
  }
  override val warningRegistrar = object : WarningRegistrar {
    override fun registerCompatibilityWarning(warning: CompatibilityWarning) {
      // NO-OP
    }
  }
  override val apiUsageProcessors = emptyList<ApiUsageProcessor>()
}