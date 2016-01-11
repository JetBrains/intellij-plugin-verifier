package com.intellij.structure.errors;

/**
 * @author Sergey Patrikeev
 */
public class MissingPluginIdException extends IncorrectPluginException {
  public MissingPluginIdException() {
  }

  public MissingPluginIdException(String message) {
    super(message);
  }

  public MissingPluginIdException(String message, Throwable cause) {
    super(message, cause);
  }

  public MissingPluginIdException(Throwable cause) {
    super(cause);
  }
}
