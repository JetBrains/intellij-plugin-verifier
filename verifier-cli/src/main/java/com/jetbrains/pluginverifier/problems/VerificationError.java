package com.jetbrains.pluginverifier.problems;

/*
Checked because it's quite possibly that plugin is broken
(e.g. plugin has circular module-dependencies)
so verification fail is acceptable outcome
 */
public class VerificationError extends Exception {
  public VerificationError(String message, Throwable cause) {
    super(message, cause);
  }

  public VerificationError(String message) {
    super(message);
  }
}
