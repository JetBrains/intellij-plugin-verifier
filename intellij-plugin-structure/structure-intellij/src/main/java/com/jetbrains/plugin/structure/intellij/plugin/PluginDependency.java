package com.jetbrains.plugin.structure.intellij.plugin;

import org.jetbrains.annotations.NotNull;

public interface PluginDependency {
  @NotNull
  String getId();

  boolean isOptional();

  boolean isModule();
}
