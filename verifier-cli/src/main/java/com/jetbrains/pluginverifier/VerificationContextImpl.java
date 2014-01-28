package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.problems.ProblemLocation;
import com.jetbrains.pluginverifier.problems.ProblemSet;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Sergey Evdokimov
 */
public class VerificationContextImpl implements VerificationContext {

  private final PluginVerifierOptions options;

  private final ProblemSet problems = new ProblemSet();

  public VerificationContextImpl(PluginVerifierOptions options) {
    this.options = options;
  }

  @Override
  public PluginVerifierOptions getOptions() {
    return options;
  }

  @Override
  public void registerProblem(@NotNull Problem problem, @NotNull ProblemLocation location) {
    problems.addProblem(problem, location);
  }

  public ProblemSet getProblems() {
    return problems;
  }
}
