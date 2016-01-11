package com.intellij.structure.errors;

/**
 * @author Sergey Patrikeev
 */
public class IncorrectCompatibleBuildsException extends IncorrectPluginXmlException {
  public IncorrectCompatibleBuildsException() {
  }

  public IncorrectCompatibleBuildsException(String message) {
    super(message);
  }

  public IncorrectCompatibleBuildsException(String message, Throwable cause) {
    super(message, cause);
  }

  public IncorrectCompatibleBuildsException(Throwable cause) {
    super(cause);
  }
}
