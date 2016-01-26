package com.intellij.structure.domain;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PluginDependency {
  private final String myId;
  private final boolean myIsOptional;

  public PluginDependency(@NotNull String id, @Nullable final Boolean isOptional) {
    myId = id;
    myIsOptional = isOptional == null ? false : isOptional;
  }

  @NotNull
  public String getId() {
    return myId;
  }

  public boolean isOptional() {
    return myIsOptional;
  }

  @Override
  public String toString() {
    return myId + (myIsOptional ? " (optional)" : "");
  }
}
