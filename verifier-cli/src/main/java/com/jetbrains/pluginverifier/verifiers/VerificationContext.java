package com.jetbrains.pluginverifier.verifiers;

import com.intellij.structure.domain.Ide;
import com.intellij.structure.domain.Jdk;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.Problem;
import org.jetbrains.annotations.NotNull;

/**
 * @author Sergey Evdokimov
 */
public interface VerificationContext {
  PluginVerifierOptions getVerifierOptions();

  void registerProblem(@NotNull Problem problem, @NotNull ProblemLocation location);

  Plugin getPlugin();

  Ide getIde();

  Jdk getJdk();

  Resolver getExternalClassPath();
}
