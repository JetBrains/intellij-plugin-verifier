package com.jetbrains.pluginverifier.domain;

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
}
