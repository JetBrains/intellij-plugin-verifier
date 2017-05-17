package com.intellij.structure.plugin;

import com.intellij.structure.problems.PluginProblem;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface PluginCreationSuccess extends PluginCreationResult {
  @NotNull
  Plugin getPlugin();

  @NotNull
  List<PluginProblem> getWarnings();
}
