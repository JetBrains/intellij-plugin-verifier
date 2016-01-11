package com.jetbrains.pluginverifier;

import com.intellij.structure.domain.Ide;
import com.intellij.structure.domain.IdeRuntime;
import com.intellij.structure.pool.ClassPool;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.results.ProblemLocation;
import org.jetbrains.annotations.NotNull;

/**
 * @author Sergey Evdokimov
 */
public interface VerificationContext {
  PluginVerifierOptions getVerifierOptions();

  void registerProblem(@NotNull Problem problem, @NotNull ProblemLocation location);

  Ide getIde();

  IdeRuntime getIdeRuntime();

  ClassPool getExternalClassPath();
}
