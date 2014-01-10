package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.domain.IdeaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * @author Dennis.Ushakov
 */
public interface Verifier {
  public void verify(@NotNull IdeaPlugin plugin, @NotNull VerificationContext ctx);
}
