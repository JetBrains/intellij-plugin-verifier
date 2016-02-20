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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PluginDependency that = (PluginDependency) o;

    return myIsOptional == that.myIsOptional && myId.equals(that.myId);

  }

  @Override
  public int hashCode() {
    int result = myId.hashCode();
    result = 31 * result + (myIsOptional ? 1 : 0);
    return result;
  }
}
