package com.intellij.structure.plugin;

import com.intellij.structure.problems.PluginProblem;
import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface PluginCreationSuccess extends PluginCreationResult {
  @NotNull
  Plugin getPlugin();

  Resolver getClassesResolver();

  @NotNull
  List<PluginProblem> getWarnings();
}
