package com.intellij.structure.domain;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface PluginCreationSuccess extends PluginCreationResult {
  @NotNull
  Plugin getPlugin();

  @NotNull
  List<PluginProblem> getWarnings();
}
