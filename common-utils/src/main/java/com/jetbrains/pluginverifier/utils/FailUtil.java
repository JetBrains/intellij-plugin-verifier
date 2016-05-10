package com.jetbrains.pluginverifier.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by Sergey Patrikeev
 */
public class FailUtil {

  public static RuntimeException fail(@NotNull Throwable cause) {
    return fail(cause.getLocalizedMessage(), cause);
  }

  public static RuntimeException fail(@NotNull String message) {
    return fail(message, null);
  }

  public static RuntimeException fail(@NotNull String message, @Nullable Throwable cause) {
    System.err.println("Fatal error " + message);
    if (cause != null) {
      cause.printStackTrace();
    }
    return new RuntimeException(message, cause);
  }

  public static void assertTrue(boolean condition, @NotNull String errorMessage) {
    if (!condition) {
      throw new RuntimeException(errorMessage);
    }
  }

  public static void assertTrue(boolean condition) {
    assertTrue(condition, "assertion failed");
  }
}
