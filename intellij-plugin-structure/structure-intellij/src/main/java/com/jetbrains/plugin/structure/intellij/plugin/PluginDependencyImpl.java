package com.jetbrains.plugin.structure.intellij.plugin;

import org.jetbrains.annotations.NotNull;

public class PluginDependencyImpl implements PluginDependency {
  private final String myId;
  private final boolean myIsOptional;
  private final boolean myIsModule;

  private static final long serialVersionUID = 0;

  public PluginDependencyImpl(@NotNull String id, @NotNull final Boolean isOptional, boolean isModule) {
    myId = id;
    myIsOptional = isOptional;
    myIsModule = isModule;
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
  public boolean isModule() {
    return myIsModule;
  }

  @Override
  public String toString() {
    return (myIsModule ? "module " : "") + myId + (myIsOptional ? " (optional)" : "");
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
