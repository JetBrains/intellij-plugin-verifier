package com.intellij.structure.impl.errors;

import com.intellij.structure.errors.IncorrectPluginException;

/**
 * @author Sergey Patrikeev
 */
public class MissingPluginIdException extends IncorrectPluginException {

  public MissingPluginIdException(String message) {
    super(message);
  }

}
