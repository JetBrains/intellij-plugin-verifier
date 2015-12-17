package com.jetbrains.pluginverifier.problems;

/**
 * @author Sergey Patrikeev
 */

import org.jetbrains.annotations.NotNull;

/**
 * This problem is not saved to report-file: it's only shown on verification status page
 */
public class NoCompatibleUpdatesProblem extends Problem {

  private String myPlugin;
  private String myIdeVersion;

  public NoCompatibleUpdatesProblem() {

  }

  public NoCompatibleUpdatesProblem(@NotNull String plugin, @NotNull String ideVersion) {
    myPlugin = plugin;
    myIdeVersion = ideVersion;
  }

  @Override
  public String getDescriptionPrefix() {
    return "For " + myPlugin + " there are no updates compatible with " + myIdeVersion + " in the Plugin Repository";
  }

  @Override
  public String getDescription() {
    return getDescriptionPrefix();
  }

  public String getIdeVersion() {
    return myIdeVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    NoCompatibleUpdatesProblem that = (NoCompatibleUpdatesProblem) o;

    return myPlugin.equals(that.myPlugin);

  }

  @Override
  public int hashCode() {
    return myPlugin.hashCode();
  }
}
