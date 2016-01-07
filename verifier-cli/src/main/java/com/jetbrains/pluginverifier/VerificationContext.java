package com.jetbrains.pluginverifier;

import com.intellij.structure.domain.Ide;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.results.ProblemLocation;
import org.jetbrains.annotations.NotNull;

/**
 * @author Sergey Evdokimov
 */
public interface VerificationContext {
  PluginVerifierOptions getOptions();

  void registerProblem(@NotNull Problem problem, @NotNull ProblemLocation location);

  Ide getIde();
}
