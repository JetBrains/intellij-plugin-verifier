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

  @Override
  public String getDescriptionPrefix() {
    return "missing plugin dependency";
  }

  @Override
  public String getDescription() {
    return getDescriptionPrefix() + " of a plugin " + myPlugin + " (" + myMissDescription + ")";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MissingDependencyProblem that = (MissingDependencyProblem) o;

    if (myMissingId != null ? !myMissingId.equals(that.myMissingId) : that.myMissingId != null) return false;
    return myMissDescription != null ? myMissDescription.equals(that.myMissDescription) : that.myMissDescription == null;

  }

  @Override
  public int hashCode() {
    int result = 1001010;
    result = 31 * result + (myMissingId != null ? myMissingId.hashCode() : 0);
    result = 31 * result + (myMissDescription != null ? myMissDescription.hashCode() : 0);
    return result;
  }

  public String getPlugin() {
    return myPlugin;
  }

  public void setPlugin(String plugin) {
    myPlugin = plugin;
  }
}
