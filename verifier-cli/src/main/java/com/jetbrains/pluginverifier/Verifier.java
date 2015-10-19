package com.jetbrains.pluginverifier;

import com.intellij.structure.domain.IdeaPlugin;
import com.jetbrains.pluginverifier.problems.VerificationError;
import org.jetbrains.annotations.NotNull;

/**
 * @author Dennis.Ushakov
 */
public interface Verifier {
  void verify(@NotNull IdeaPlugin plugin, @NotNull VerificationContext ctx) throws VerificationError;
}
