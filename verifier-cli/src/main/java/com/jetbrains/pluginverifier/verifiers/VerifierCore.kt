package com.jetbrains.pluginverifier.verifiers

import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.Plugin
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.VOptions
import com.jetbrains.pluginverifier.results.ProblemSet

/**
 * @author Sergey Patrikeev
 */
object VerifierCore {

  /**
   * @return problems of plugin against specified IDEA
   */
  @JvmStatic
  fun verifyPlugin(plugin: Plugin,
                   ide: Ide,
                   ideResolver: Resolver,
                   jdkResolver: Resolver,
                   externalClassPath: Resolver?,
                   options: VOptions): ProblemSet {

    val ctx = VerificationContextImpl(plugin, ide, ideResolver, jdkResolver, externalClassPath, options)

    Verifiers.processAllVerifiers(ctx)

    val problemSet = ctx.problemSet

    printProblemsOnStdout(problemSet)

    return problemSet

  }

  private fun printProblemsOnStdout(problemSet: ProblemSet) {
    println(if (problemSet.isEmpty) "is OK" else " has " + problemSet.count() + " errors")
    problemSet.printProblems(System.out, "")

    val allProblems = problemSet.allProblems

    for (problem in allProblems) {
      val description = StringBuilder(problem.description)
      val locations = problemSet.getLocations(problem)
      if (!locations.isEmpty()) {
        description.append(" at ").append(locations.iterator().next())
        val remaining = locations.size - 1
        if (remaining > 0) {
          description.append(" and ").append(remaining).append(" more location")
          if (remaining > 1) description.append("s")
        }
      }
      System.err.println(description)
    }
  }

}