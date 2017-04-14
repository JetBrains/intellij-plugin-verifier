package com.intellij.structure.impl.domain;

import com.intellij.structure.plugin.Plugin;
import com.intellij.structure.plugin.PluginCreationSuccess;
import com.intellij.structure.problems.PluginProblem;
import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PluginCreationSuccessImpl implements PluginCreationSuccess {
  @NotNull private final Plugin myPlugin;
  @NotNull private final List<PluginProblem> myWarnings;
  @Nullable
  private final Resolver myResolver;

  public PluginCreationSuccessImpl(@NotNull Plugin plugin,
                                   @NotNull List<PluginProblem> warnings,
                                   @Nullable Resolver resolver) {
    myPlugin = plugin;
    myWarnings = warnings;
    myResolver = resolver;

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

  @Nullable
  @Override
  public Resolver getClassesResolver() {
    return myResolver;
  }

  @Override
  @NotNull
  public List<PluginProblem> getWarnings() {
    return myWarnings;
  }
}
