package com.intellij.openapi.components;

import com.intellij.util.xmlb.annotations.Attribute;

public final class ServiceDescriptor {
  @Attribute
  public String serviceInterface;

  @Attribute
  public String serviceImplementation;

  public String getInterface() {
    return null;
  }

  public String getImplementation() {
    return null;
  }
}