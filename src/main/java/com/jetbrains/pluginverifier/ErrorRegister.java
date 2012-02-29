package com.jetbrains.pluginverifier;

public interface ErrorRegister {
  void registerError(final String className, final String error);
}
