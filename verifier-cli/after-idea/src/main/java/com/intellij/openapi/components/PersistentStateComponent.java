package com.intellij.openapi.components;

public interface PersistentStateComponent<T> {
  T getState();
}
