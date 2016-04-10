package com.jetbrains.pluginverifier.dependencies;

/**
 * @author Sergey Patrikeev
 */
public class CyclicDependencyError extends DependenciesError {

  private final String myCycle;

  public CyclicDependencyError(String cycle) {
    myCycle = cycle;
  }

  public String getCycle() {
    return myCycle;
  }
}
