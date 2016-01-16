package com.intellij.structure.errors;

/**
 * @author Sergey Patrikeev
 */
public abstract class IncorrectPluginException extends RuntimeException {
  protected IncorrectPluginException() {
  }

  protected IncorrectPluginException(String message) {
    super(message);
  }

  protected IncorrectPluginException(String message, Throwable cause) {
    super(message, cause);
  }

  protected IncorrectPluginException(Throwable cause) {
    super(cause);
  }
}
