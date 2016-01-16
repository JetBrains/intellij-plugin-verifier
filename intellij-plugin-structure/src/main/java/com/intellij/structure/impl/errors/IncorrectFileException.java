package com.intellij.structure.impl.errors;

import com.intellij.structure.errors.IncorrectPluginException;

/**
 * @author Sergey Patrikeev
 */
class IncorrectFileException extends IncorrectPluginException {

  IncorrectFileException(String message) {
    super(message);
  }

  IncorrectFileException(String message, Throwable cause) {
    super(message, cause);
  }

}
