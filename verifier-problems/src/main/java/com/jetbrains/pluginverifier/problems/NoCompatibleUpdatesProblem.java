package com.jetbrains.pluginverifier.problems;


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

  @NotNull
  @Override
  public String getDescriptionPrefix() {
    return "For " + myPlugin + " there are no updates compatible with " + myIdeVersion + " in the Plugin Repository" + (myDetails != null ? " " + myDetails : "");
  }

  @NotNull
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

  public String getDetails() {
    return myDetails;
  }

  public void setDetails(String details) {
    myDetails = details;
  }
}
