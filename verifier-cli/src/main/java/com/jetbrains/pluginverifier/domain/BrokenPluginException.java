package com.jetbrains.pluginverifier.domain;

/**
 * @author Sergey Evdokimov
 */
public class BrokenPluginException extends Exception {

  public BrokenPluginException() {
  }

  public BrokenPluginException(String message) {
    super(message);
  }

  public BrokenPluginException(String message, Throwable cause) {
    super(message, cause);
  }

  public BrokenPluginException(Throwable cause) {
    super(cause);
  }
}
