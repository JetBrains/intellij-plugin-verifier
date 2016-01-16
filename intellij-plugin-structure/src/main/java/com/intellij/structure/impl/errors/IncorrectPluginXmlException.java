package com.intellij.structure.impl.errors;

/**
 * @author Sergey Patrikeev
 */
public class IncorrectPluginXmlException extends IncorrectFileException {

  IncorrectPluginXmlException(String message) {
    super(message);
  }

  public IncorrectPluginXmlException(String message, Throwable cause) {
    super(message, cause);
  }

}
