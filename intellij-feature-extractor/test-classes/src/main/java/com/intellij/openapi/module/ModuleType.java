package com.intellij.openapi.module;

public abstract class ModuleType {

  private final String myId;

  protected ModuleType(String id) {
    myId = id;
  }
}
