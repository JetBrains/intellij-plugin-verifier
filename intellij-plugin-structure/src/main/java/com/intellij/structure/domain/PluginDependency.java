package com.intellij.structure.domain;

import org.jetbrains.annotations.NotNull;

/**
 * @author Sergey Patrikeev
 */
public interface PluginDependency {
  @NotNull
  String getId();

  boolean isOptional();
}
