package com.jetbrains.pluginverifier.domain;

/**
 * @author Sergey Evdokimov
 */
public class FatalError extends RuntimeException {

  public FatalError(String message) {
    super(message);
  }
}
