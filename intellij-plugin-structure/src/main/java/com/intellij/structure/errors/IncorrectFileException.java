package com.intellij.structure.errors;

/**
 * @author Sergey Patrikeev
 */
class IncorrectFileException extends IncorrectPluginException {

  IncorrectFileException(String message) {
    super(message);
  }

  IncorrectFileException() {
  }

  IncorrectFileException(String message, Throwable cause) {

  }

  IncorrectFileException(Throwable cause) {

  }
}
