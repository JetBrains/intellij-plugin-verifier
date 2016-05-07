package com.jetbrains.pluginverifier.utils;

import com.intellij.structure.domain.Ide;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.results.ProblemSet;
import com.jetbrains.pluginverifier.verifiers.PluginVerifierOptions;
import com.jetbrains.pluginverifier.verifiers.VerificationContextImpl;
import com.jetbrains.pluginverifier.verifiers.Verifiers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * @author Sergey Patrikeev
 */
public class Verification {

  /**
   * @return problems of plugin against specified IDEA
   */
  @NotNull
  public static ProblemSet verifyPlugin(@NotNull Plugin plugin,
                                        @NotNull Ide ide,
                                        @NotNull Resolver ideResolver,
                                        @NotNull Resolver jdkResolver,
                                        @Nullable Resolver externalClassPath,
                                        @NotNull PluginVerifierOptions options) {

    VerificationContextImpl ctx = new VerificationContextImpl(plugin, ide, ideResolver, jdkResolver, externalClassPath, options);

    Verifiers.processAllVerifiers(ctx);

    ProblemSet problemSet = ctx.getProblemSet();

    printProblemsOnStdout(problemSet);

    return problemSet;

  }

  private static void printProblemsOnStdout(ProblemSet problemSet) {
    System.out.println(problemSet.isEmpty() ? "is OK" : " has " + problemSet.count() + " errors");
    problemSet.printProblems(System.out, "");

    Set<Problem> allProblems = problemSet.getAllProblems();

    for (Problem problem : allProblems) {
      StringBuilder description = new StringBuilder(problem.getDescription());
      Set<ProblemLocation> locations = problemSet.getLocations(problem);
      if (!locations.isEmpty()) {
        description.append(" at ").append(locations.iterator().next());
        int remaining = locations.size() - 1;
        if (remaining > 0) {
          description.append(" and ").append(remaining).append(" more location");
          if (remaining > 1) description.append("s");
        }
      }
      System.err.println(description);
    }
  }
}
