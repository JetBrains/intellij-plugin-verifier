package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.error.VerificationError;
import org.jetbrains.annotations.NotNull;

/**
 * @author Dennis.Ushakov
 */
public interface Verifier {
  void verify(@NotNull VerificationContext ctx) throws VerificationError;
}
