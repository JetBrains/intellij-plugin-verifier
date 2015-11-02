package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.TeamCityLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by Sergey Patrikeev
 */
public class FailUtil {

  public static RuntimeException fail(@NotNull Throwable cause) {
    throw fail(cause.getLocalizedMessage(), cause);
  }

  public static RuntimeException fail(@NotNull String message) {
    throw fail(message, null);
  }

  public static RuntimeException fail(@NotNull String message, @Nullable Throwable cause) {
    throw fail(message, cause, null);
  }

  public static RuntimeException fail(@NotNull String message, @Nullable Throwable cause, @Nullable TeamCityLog log) {
    System.err.println("Fatal error " + message);
    if (log != null) {
      log.messageError("Fatal error " + message);
    }
    throw new RuntimeException(message, cause);
  }
}
