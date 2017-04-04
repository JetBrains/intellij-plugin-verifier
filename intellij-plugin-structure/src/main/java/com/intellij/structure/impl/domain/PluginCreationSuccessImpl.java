package com.intellij.structure.impl.domain;

import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginCreationSuccess;
import com.intellij.structure.domain.PluginProblem;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PluginCreationSuccessImpl implements PluginCreationSuccess {
  @NotNull private final Plugin myPlugin;
  @NotNull private final List<PluginProblem> myWarnings;

  public PluginCreationSuccessImpl(@NotNull Plugin plugin, @NotNull List<PluginProblem> warnings) {
    myPlugin = plugin;
    myWarnings = warnings;

    for (PluginProblem warning : myWarnings) {
      if (warning.getLevel() == PluginProblem.Level.ERROR) {
        throw new IllegalArgumentException("PluginCreationFail must be returned in case of errors");
      }
    }
  }

  @Override
  @NotNull
  public Plugin getPlugin() {
    return myPlugin;
  }

  @Override
  @NotNull
  public List<PluginProblem> getWarnings() {
    return myWarnings;
  }
}
