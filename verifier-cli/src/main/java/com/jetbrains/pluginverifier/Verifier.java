package com.jetbrains.pluginverifier;

import com.intellij.structure.domain.IdeaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * @author Dennis.Ushakov
 */
public interface Verifier {
  public void verify(@NotNull IdeaPlugin plugin, @NotNull VerificationContext ctx);
}
