package com.intellij.structure.errors;

/**
 * @author Sergey Patrikeev
 */
public class MultiplePluginXmlException extends IncorrectStructureException {
  public MultiplePluginXmlException() {
  }

  public MultiplePluginXmlException(String message) {
    super(message);
  }

  public MultiplePluginXmlException(String message, Throwable cause) {
    super(message, cause);
  }

  public MultiplePluginXmlException(Throwable cause) {
    super(cause);
  }
}
