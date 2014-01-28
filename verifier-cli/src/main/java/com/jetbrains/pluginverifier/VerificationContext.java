package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.problems.ProblemLocation;
import org.jetbrains.annotations.NotNull;

/**
 * @author Sergey Evdokimov
 */
public interface VerificationContext {
  PluginVerifierOptions getOptions();

  void registerProblem(@NotNull Problem problem, @NotNull ProblemLocation location);
}
