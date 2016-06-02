package com.jetbrains.pluginverifier.utils.dependencies;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Sergey Patrikeev
 */
public class MissingReason {
  @NotNull
  private final String myReason;
  @Nullable
  private final Exception myException;

  public MissingReason(@NotNull String reason, @Nullable Exception exception) {
    myReason = reason;
    myException = exception;
  }

  @NotNull
  public String getReason() {
    return myReason;
  }

  @Nullable
  public Exception getException() {
    return myException;
  }
}
