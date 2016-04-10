package com.jetbrains.pluginverifier;

import org.jetbrains.annotations.NotNull;

/**
 * @author Dennis.Ushakov
 */
public interface Verifier {
  void verify(@NotNull VerificationContext ctx);
}
