package com.jetbrains.pluginverifier.tests.filter

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ResultHolder
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.parameters.filtering.DocumentedProblemsFilter
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.parameters.filtering.documented.DocumentedProblem
import com.jetbrains.pluginverifier.repository.PluginIdAndVersion
import com.jetbrains.pluginverifier.results.hierarchy.ClassHierarchy
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.tests.mocks.EmptyClassResolver
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext
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
      context: PluginVerificationContext
  ) {
    val problems = problemAndItsDocumentation.map { it.first }
    val documentedProblems = problemAndItsDocumentation.map { it.second }

    val problemsFilter = DocumentedProblemsFilter(documentedProblems)

    for (problem in problems) {
      val shouldReportProblem = problemsFilter.shouldReportProblem(
          problem,
          context
      )
      if (shouldReportProblem !is ProblemsFilter.Result.Ignore) {
        Assert.fail("Problem is not ignored:\n$problem")
      }
    }
  }

  fun createSimpleVerificationContext() = PluginVerificationContext(
      PluginIdAndVersion("pluginId", "1.0"),
      VerificationTarget.Ide(IdeVersion.createIdeVersion("IU-145.1")),
      ResultHolder(),
      false,
      emptyList(),
      EmptyClassResolver,
      emptyList()
  )

}