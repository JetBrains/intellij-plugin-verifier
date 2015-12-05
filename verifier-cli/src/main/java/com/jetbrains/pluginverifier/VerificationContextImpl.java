package com.jetbrains.pluginverifier;

import com.intellij.structure.domain.Idea;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.results.ProblemLocation;
import com.jetbrains.pluginverifier.results.ProblemSet;
import org.jetbrains.annotations.NotNull;

/**
 * @author Sergey Evdokimov
 */
public class VerificationContextImpl implements VerificationContext {

  private final PluginVerifierOptions options;

  private final ProblemSet problems = new ProblemSet();

  private final Idea ide;

  public VerificationContextImpl(PluginVerifierOptions options, Idea ide) {
    this.options = options;
    this.ide = ide;
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

  public Idea getIde() {
    return ide;
  }
}
