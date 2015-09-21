package com.intellij.structure.domain;

import com.google.common.base.Strings;

public class PluginDependency {
  private final String myId;
  private final Boolean myIsOptional;

  public PluginDependency(final String id, final Boolean isOptional) {
    myId = id;
    myIsOptional = isOptional;
  }

  public String getId() {
    return myId;
  }

  public Boolean isOptional() {
    return myIsOptional;
  }

  @Override
  public String toString() {
    return Strings.nullToEmpty(myId);
  }
}
