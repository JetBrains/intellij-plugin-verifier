package com.jetbrains.pluginverifier.tests.filter

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ResultHolder
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.parameters.filtering.DocumentedProblemsFilter
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.parameters.filtering.documented.DocumentedProblem
import com.jetbrains.pluginverifier.repository.PluginIdAndVersion
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.tests.mocks.EmptyClsResolver
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import org.junit.Assert

/**
 * These tests assert that the [documented] [DocumentedProblemsFilter]
 * problems are indeed excluded from the verification reports.
 */
abstract class BaseDocumentedProblemsReportingTest {

  fun assertProblemsDocumented(
      problemAndItsDocumentation: List<Pair<CompatibilityProblem, DocumentedProblem>>,
      verificationContext: VerificationContext
  ) {
    val problems = problemAndItsDocumentation.map { it.first }
    val documentedProblems = problemAndItsDocumentation.map { it.second }

    val problemsFilter = DocumentedProblemsFilter(documentedProblems)

    for (problem in problems) {
      val shouldReportProblem = problemsFilter.shouldReportProblem(
          problem,
          verificationContext
      )
      if (shouldReportProblem !is ProblemsFilter.Result.Ignore) {
        Assert.fail("Problem is not ignored:\n$problem")
      }
    }
  }

  fun createSimpleVerificationContext() = VerificationContext(
      PluginIdAndVersion("pluginId", "1.0"),
      VerificationTarget.Ide(IdeVersion.createIdeVersion("IU-145.1")),
      ResultHolder(),
      false,
      emptyList(),
      EmptyClsResolver
  )

}