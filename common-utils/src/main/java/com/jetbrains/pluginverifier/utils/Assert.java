package com.jetbrains.pluginverifier.utils;

import org.jetbrains.annotations.NotNull;

/**
 * @author Sergey Patrikeev
 */
public class Assert {
  public static void assertTrue(boolean condition, @NotNull String errorMessage) {
    if (!condition) {
      throw new AssertionError(errorMessage);
    }
  }

  public static void assertTrue(boolean condition) {
    assertTrue(condition, "assertion failed");
  }
}
