package com.intellij.structure.plugin;

import org.jetbrains.annotations.NotNull;

/**
 * @author Sergey Patrikeev
 */
public interface PluginDependency {
  @NotNull
  String getId();

  boolean isOptional();
}
