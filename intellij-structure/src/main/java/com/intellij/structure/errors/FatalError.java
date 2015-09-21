package com.intellij.structure.errors;

/**
 * @author Sergey Evdokimov
 */
public class FatalError extends RuntimeException {

  public FatalError(String message) {
    super(message);
  }
}
