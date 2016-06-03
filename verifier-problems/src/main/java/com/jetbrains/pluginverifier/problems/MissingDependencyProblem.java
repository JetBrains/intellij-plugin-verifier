package com.jetbrains.pluginverifier.problems;

import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MissingDependencyProblem extends Problem {

  private String myPlugin;
  private String myMissingId;
  private String myMissDescription;

  public MissingDependencyProblem() {
  }

  public MissingDependencyProblem(String plugin, @NotNull String missingId, @NotNull String missDescription) {
    myPlugin = plugin;
    myMissingId = missingId;
    myMissDescription = missDescription;
  }

  public String getMissingId() {
    return myMissingId;
  }

  public void setMissingId(String missingId) {
    myMissingId = missingId;
  }

  public String getMissDescription() {
    return myMissDescription;
  }

  public void setMissDescription(String missDescription) {
    myMissDescription = missDescription;
  }

  @NotNull
  @Override
  public String getDescriptionPrefix() {
    return "missing plugin dependency";
  }

  @NotNull
  @Override
  public String getDescription() {
    return getDescriptionPrefix() + " of a plugin " + myPlugin + " (" + myMissDescription + ")";
  }

  public String getPlugin() {
    return myPlugin;
  }

  public void setPlugin(String plugin) {
    myPlugin = plugin;
  }
}
