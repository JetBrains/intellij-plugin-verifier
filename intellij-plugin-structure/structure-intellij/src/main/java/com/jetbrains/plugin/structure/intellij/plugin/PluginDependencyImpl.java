/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PluginDependencyImpl implements PluginDependency {
  private final String myId;
  private boolean myIsOptional;
  private final boolean myIsModule;

  public PluginDependencyImpl(@NotNull String id, boolean isOptional, boolean isModule) {
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
    if (o == null || getClass() != o.getClass()) return false;
    PluginDependencyImpl that = (PluginDependencyImpl) o;
    return myIsOptional == that.myIsOptional && myIsModule == that.myIsModule && Objects.equals(myId, that.myId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myId, myIsOptional, myIsModule);
  }

  @Override
  public @NotNull PluginDependency asOptional() {
    return new PluginDependencyImpl(myId, /* isOptional */ true, myIsModule);
  }
}
