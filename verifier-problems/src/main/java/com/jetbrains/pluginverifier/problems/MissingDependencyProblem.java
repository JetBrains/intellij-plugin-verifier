package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MissingDependencyProblem extends Problem {

  @SerializedName("plugin")
  private String myPlugin;
  @SerializedName("missingId")
  private String myMissingId;
  @SerializedName("description")
  private String myMissDescription;

  public MissingDependencyProblem() {
  }

  public MissingDependencyProblem(@NotNull String plugin, @NotNull String missingId, @NotNull String missDescription) {
    Preconditions.checkNotNull(plugin);
    Preconditions.checkNotNull(missingId);
    Preconditions.checkNotNull(missDescription);
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
  public String getDescription() {
    return "missing plugin dependency" + " of a plugin " + myPlugin + " (" + myMissDescription + ")";
  }

  public String getPlugin() {
    return myPlugin;
  }

  public void setPlugin(String plugin) {
    myPlugin = plugin;
  }

}
