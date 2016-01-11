package com.intellij.structure.errors;

/**
 * @author Sergey Patrikeev
 */
public class IncorrectPluginXmlException extends IncorrectFileException {
  public IncorrectPluginXmlException() {
  }

  public IncorrectPluginXmlException(String message) {
    super(message);
  }

  public IncorrectPluginXmlException(String message, Throwable cause) {
    super(message, cause);
  }

  public IncorrectPluginXmlException(Throwable cause) {
    super(cause);
  }
}
