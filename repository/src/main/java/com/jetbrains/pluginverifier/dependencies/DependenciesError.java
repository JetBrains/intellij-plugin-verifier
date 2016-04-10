package com.jetbrains.pluginverifier.dependencies;

/**
 * @author Sergey Patrikeev
 */
public abstract class DependenciesError extends Exception {

  public DependenciesError() {
  }

  public DependenciesError(Throwable cause) {
    super(cause);
  }
}
