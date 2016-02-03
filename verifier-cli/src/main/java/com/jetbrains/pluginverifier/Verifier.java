package com.jetbrains.pluginverifier;

import com.intellij.structure.domain.Plugin;
import com.jetbrains.pluginverifier.error.VerificationError;
import org.jetbrains.annotations.NotNull;

/**
 * @author Dennis.Ushakov
 */
public interface Verifier {
  void verify(@NotNull Plugin plugin, @NotNull VerificationContext ctx) throws VerificationError;
}
