package com.jetbrains.pluginverifier.problems;

/**
 * @author Sergey Patrikeev
 */
public class VerificationError extends RuntimeException {
  public VerificationError(String message, Throwable cause) {
    super(message, cause);
  }

  public VerificationError(String message) {
    super(message);
  }
}
