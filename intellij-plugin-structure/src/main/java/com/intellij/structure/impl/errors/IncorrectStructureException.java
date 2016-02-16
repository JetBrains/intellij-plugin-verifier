package com.intellij.structure.impl.errors;

import com.intellij.structure.errors.IncorrectPluginException;

/**
 * @author Sergey Patrikeev
 */
public class IncorrectStructureException extends IncorrectPluginException {

  public IncorrectStructureException(String message) {
    super(message);
  }

  public IncorrectStructureException(String message, Throwable cause) {
    super(message, cause);
  }
}
