package com.intellij.execution.configurations;

import javax.swing.*;

public abstract class ConfigurationTypeBase implements ConfigurationType {

  private final String myId;

  protected ConfigurationTypeBase(String id, String displayName, String description, Icon icon) {
    myId = id;
  }

  public String getId() {
    return myId;
  }
}