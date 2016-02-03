package com.intellij.structure.impl.errors;

import com.intellij.structure.errors.IncorrectPluginException;

/**
 * @author Sergey Patrikeev
 */
public class MissingPluginXmlException extends IncorrectPluginException {
  public MissingPluginXmlException(String message) {
    super(message);
  }

}
