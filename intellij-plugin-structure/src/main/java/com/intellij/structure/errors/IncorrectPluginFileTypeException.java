package com.intellij.structure.errors;

/**
 * @author Sergey Patrikeev
 */
public class IncorrectPluginFileTypeException extends IncorrectPluginException {
  public IncorrectPluginFileTypeException() {
  }

  public IncorrectPluginFileTypeException(String message) {
    super(message);
  }

  public IncorrectPluginFileTypeException(String message, Throwable cause) {
    super(message, cause);
  }

  public IncorrectPluginFileTypeException(Throwable cause) {
    super(cause);
  }
}
