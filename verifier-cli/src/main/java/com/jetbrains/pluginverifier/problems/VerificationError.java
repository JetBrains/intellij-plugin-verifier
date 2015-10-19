package com.jetbrains.pluginverifier.problems;

/**
 * @author Sergey Patrikeev
 */
public class VerificationError extends Exception {

  public VerificationError(String message) {
    super(message);
  }

  public VerificationError(String message, Throwable cause) {
    super(message, cause);
  }
}
