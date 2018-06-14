package com.jetbrains.plugin.structure.intellij.plugin;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public interface PluginDependency extends Serializable {
  @NotNull
  String getId();

  boolean isOptional();

  boolean isModule();
}
