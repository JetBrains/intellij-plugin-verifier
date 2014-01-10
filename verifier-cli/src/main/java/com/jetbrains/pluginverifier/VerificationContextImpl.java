package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.problems.Problem;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sergey Evdokimov
 */
public class VerificationContextImpl implements VerificationContext {

  private final PluginVerifierOptions options;

  private final List<Problem> problems = new ArrayList<Problem>();

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

  public List<Problem> getProblems() {
    return problems;
  }
}
