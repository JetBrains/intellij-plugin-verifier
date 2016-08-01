package com.jetbrains.pluginverifier.problems;

import org.jetbrains.annotations.NotNull;

/**
 * Represents possible JVM visibility constants.
 *
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
