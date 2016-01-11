package com.intellij.structure.errors;

/**
 * @author Sergey Patrikeev
 */
public class EmptyLibDirectoryException extends IncorrectStructureException {
  public EmptyLibDirectoryException() {
  }

  public EmptyLibDirectoryException(String message) {
    super(message);
  }

  public EmptyLibDirectoryException(String message, Throwable cause) {
    super(message, cause);
  }

  public EmptyLibDirectoryException(Throwable cause) {
    super(cause);
  }
}
