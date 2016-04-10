package com.jetbrains.pluginverifier.problems;

/**
 * @author Sergey Patrikeev
 */

import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class NoCompatibleUpdatesProblem extends Problem {

  private String myPlugin;
  private String myIdeVersion;
  private String myDetails;

  public NoCompatibleUpdatesProblem() {

  }

  public NoCompatibleUpdatesProblem(@NotNull String plugin, @NotNull String ideVersion) {
    this(plugin, ideVersion, null);
  }

  public NoCompatibleUpdatesProblem(@NotNull String plugin, @NotNull String ideVersion, String details) {
    myPlugin = plugin;
    myIdeVersion = ideVersion;
    myDetails = details;
  }

  @Override
  public String getDescriptionPrefix() {
    return "For " + myPlugin + " there are no updates compatible with " + myIdeVersion + " in the Plugin Repository" + (myDetails != null ? " " + myDetails : "");
  }

  @Override
  public String getDescription() {
    return getDescriptionPrefix();
  }

  public String getIdeVersion() {
    return myIdeVersion;
  }

  public void setIdeVersion(String ideVersion) {
    myIdeVersion = ideVersion;
  }

  public void setPlugin(String plugin) {
    myPlugin = plugin;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    NoCompatibleUpdatesProblem that = (NoCompatibleUpdatesProblem) o;

    if (myPlugin != null ? !myPlugin.equals(that.myPlugin) : that.myPlugin != null) return false;
    return myIdeVersion != null ? myIdeVersion.equals(that.myIdeVersion) : that.myIdeVersion == null;

  }

  @Override
  public int hashCode() {
    int result = 1;
    result = 31 * result + (myPlugin != null ? myPlugin.hashCode() : 0);
    result = 31 * result + (myIdeVersion != null ? myIdeVersion.hashCode() : 0);
    return result;
  }

  public String getDetails() {
    return myDetails;
  }

  public void setDetails(String details) {
    myDetails = details;
  }
}
