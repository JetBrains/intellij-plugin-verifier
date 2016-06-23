package com.jetbrains.pluginverifier.verifiers;

import com.intellij.structure.domain.Ide;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.api.VOptions;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.Problem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Sergey Evdokimov
 */
public interface VerificationContext {
  @NotNull
  VOptions getVerifierOptions();

  void registerProblem(@NotNull Problem problem, @NotNull ProblemLocation location);

  @NotNull
  String getOverview();

  @NotNull
  Plugin getPlugin();

  @NotNull
  Ide getIde();

  @NotNull
  Resolver getIdeResolver();

  @NotNull
  Resolver getJdkResolver();

  @Nullable
  Resolver getExternalClassPath();
}
