package com.jetbrains.pluginverifier.tests.filter

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.filtering.documented.DocumentedProblem
import com.jetbrains.pluginverifier.filtering.documented.DocumentedProblemsFilter
import com.jetbrains.pluginverifier.results.hierarchy.ClassHierarchy
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.ProblemRegistrar
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.packages.DefaultPackageFilter
import com.jetbrains.pluginverifier.verifiers.packages.PackageFilter
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning
import com.jetbrains.pluginverifier.warnings.WarningRegistrar
import org.junit.Assert

/**
 * These tests assert that the documented problems are indeed excluded from the verification reports.
 */
abstract class BaseDocumentedProblemsReportingTest {

  protected companion object {
    val JAVA_LANG_OBJECT_HIERARCHY = ClassHierarchy(
      "java/lang/Object",
      false,
      null,
      emptyList()
    )
  }

  fun assertProblemsDocumented(
    problemAndItsDocumentation: List<Pair<CompatibilityProblem, DocumentedProblem>>,
    context: VerificationContext
  ) {
    val problems = problemAndItsDocumentation.map { it.first }
    val documentedProblems = problemAndItsDocumentation.map { it.second }

    val problemsFilter = DocumentedProblemsFilter(documentedProblems)

    for (problem in problems) {
      val shouldReportProblem = problemsFilter.shouldReportProblem(problem, context)
      if (shouldReportProblem !is ProblemsFilter.Result.Ignore) {
        Assert.fail("Problem is not ignored:\n$problem")
      }
    }
  }

  fun createSimpleVerificationContext(resolver: Resolver): VerificationContext =
    object : VerificationContext {
      override val externalClassesPackageFilter: PackageFilter
        get() = DefaultPackageFilter(emptyList())

      override val classResolver: Resolver
        get() = resolver

      override val problemRegistrar: ProblemRegistrar
        get() = object : ProblemRegistrar {
          override fun registerProblem(problem: CompatibilityProblem) = Unit
        }

      override val warningRegistrar: WarningRegistrar
        get() = object : WarningRegistrar {
          override fun registerCompatibilityWarning(warning: CompatibilityWarning) = Unit
        }

      override val apiUsageProcessors: List<ApiUsageProcessor>
        get() = emptyList()
    }
}