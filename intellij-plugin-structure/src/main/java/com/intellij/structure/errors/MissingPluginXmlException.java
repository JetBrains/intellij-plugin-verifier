package com.intellij.structure.errors;

/**
 * @author Sergey Patrikeev
 */
public class MissingPluginXmlException extends IncorrectPluginException {
  public MissingPluginXmlException() {
  }

  public MissingPluginXmlException(String message) {
    super(message);
  }

  public MissingPluginXmlException(String message, Throwable cause) {
    super(message, cause);
  }

  public MissingPluginXmlException(Throwable cause) {
    super(cause);
  }
}
