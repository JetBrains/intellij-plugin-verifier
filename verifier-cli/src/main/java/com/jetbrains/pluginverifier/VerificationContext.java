package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.problems.Problem;

/**
 * @author Sergey Evdokimov
 */
public interface VerificationContext {
  PluginVerifierOptions getOptions();

  void registerProblem(Problem problem);
}
