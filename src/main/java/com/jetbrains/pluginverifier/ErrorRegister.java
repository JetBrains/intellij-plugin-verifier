package com.jetbrains.pluginverifier;

/**
 * @author Dennis.Ushakov
 */
public interface ErrorRegister {
  void registerError(final String resolverName, final String occurence, final String error);
}
