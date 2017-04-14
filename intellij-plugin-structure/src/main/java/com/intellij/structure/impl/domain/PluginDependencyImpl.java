package com.intellij.structure.impl.domain;

import com.intellij.structure.impl.beans.PluginDependencyBean;
import com.intellij.structure.plugin.PluginDependency;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PluginDependencyImpl implements PluginDependency {
  private final String myId;
  private final boolean myIsOptional;

  public PluginDependencyImpl(@NotNull String id, @Nullable final Boolean isOptional) {
    myId = id;
    myIsOptional = isOptional == null ? false : isOptional;
  }

  public PluginDependencyImpl(@NotNull PluginDependencyBean bean) {
    myId = bean.pluginId;
    myIsOptional = bean.optional;
  }

  @Override
  @NotNull
  public String getId() {
    return myId;
  }

  @Override
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

    PluginDependencyImpl that = (PluginDependencyImpl) o;

    return myIsOptional == that.myIsOptional && myId.equals(that.myId);

  }

  @Override
  public int hashCode() {
    int result = myId.hashCode();
    result = 31 * result + (myIsOptional ? 1 : 0);
    return result;
  }
}
