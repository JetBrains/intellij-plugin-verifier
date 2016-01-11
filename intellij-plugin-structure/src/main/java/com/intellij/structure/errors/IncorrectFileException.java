package com.intellij.structure.errors;

/**
 * @author Sergey Patrikeev
 */
public class IncorrectFileException extends IncorrectPluginException {
  public IncorrectFileException() {
  }

  public IncorrectFileException(String message) {
    super(message);
  }

  public IncorrectFileException(String message, Throwable cause) {
    super(message, cause);
  }

  public IncorrectFileException(Throwable cause) {
    super(cause);
  }
}
