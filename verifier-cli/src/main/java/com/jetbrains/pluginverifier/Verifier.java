package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.domain.IdeaPlugin;
import com.jetbrains.pluginverifier.util.Consumer;
import com.jetbrains.pluginverifier.problems.Problem;
import org.jetbrains.annotations.NotNull;

/**
 * @author Dennis.Ushakov
 */
public interface Verifier {
  public void verify(@NotNull IdeaPlugin plugin, @NotNull PluginVerifierOptions options, @NotNull Consumer<Problem> problemRegister);
}
