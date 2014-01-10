package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.problems.Problem;

import java.util.*;

/**
 * @author Sergey Evdokimov
 */
public class VerificationContextImpl implements VerificationContext {

  private final PluginVerifierOptions options;

  private final Set<Problem> problems = new LinkedHashSet<Problem>();

  public VerificationContextImpl(PluginVerifierOptions options) {
    this.options = options;
  }

  @Override
  public PluginVerifierOptions getOptions() {
    return options;
  }

  @Override
  public void registerProblem(Problem problem) {
    problems.add(problem);
  }

  public Collection<Problem> getProblems() {
    return problems;
  }
}
