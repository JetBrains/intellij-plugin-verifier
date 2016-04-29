package com.jetbrains.pluginverifier.problems;

import org.jetbrains.annotations.NotNull;

/**
 * @author Sergey Patrikeev
 */
public enum AccessType {
  PUBLIC("public"),
  PROTECTED("protected"),
  PACKAGE_PRIVATE("package-private"),
  PRIVATE("private");

  private final String myDescription;

  AccessType(@NotNull String description) {
    myDescription = description;
  }

  @NotNull
  public String getDescription() {
    return myDescription;
  }
}
