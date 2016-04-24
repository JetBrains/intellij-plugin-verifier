package com.jetbrains.pluginverifier.verifiers;

import org.jetbrains.annotations.NotNull;

/**
 * @author Dennis.Ushakov
 */
public interface Verifier {
  void verify(@NotNull VerificationContext ctx);
}
