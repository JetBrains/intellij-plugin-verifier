package com.intellij.structure.errors;

/**
 * @author Sergey Patrikeev
 */
public class IncorrectStructureException extends IncorrectPluginException {
  IncorrectStructureException() {
  }

  public IncorrectStructureException(String message) {
    super(message);
  }

  IncorrectStructureException(String message, Throwable cause) {
    super(message, cause);
  }

  IncorrectStructureException(Throwable cause) {
    super(cause);
  }
}
