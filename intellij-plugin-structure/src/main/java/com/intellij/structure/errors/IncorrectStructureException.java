package com.intellij.structure.errors;

/**
 * @author Sergey Patrikeev
 */
public class IncorrectStructureException extends IncorrectPluginException {
  public IncorrectStructureException() {
  }

  public IncorrectStructureException(String message) {
    super(message);
  }

  public IncorrectStructureException(String message, Throwable cause) {
    super(message, cause);
  }

  public IncorrectStructureException(Throwable cause) {
    super(cause);
  }
}
