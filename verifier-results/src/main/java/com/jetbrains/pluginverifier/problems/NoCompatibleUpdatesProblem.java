package com.jetbrains.pluginverifier.problems;


import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class NoCompatibleUpdatesProblem extends Problem {

  @SerializedName("plugin")
  private String myPlugin;
  @SerializedName("ideVersion")
  private String myIdeVersion;
  @SerializedName("details")
  private String myDetails;

  public NoCompatibleUpdatesProblem() {

  }

  public NoCompatibleUpdatesProblem(@NotNull String plugin, @NotNull String ideVersion) {
    this(plugin, ideVersion, null);
  }

  public NoCompatibleUpdatesProblem(@NotNull String plugin, @NotNull String ideVersion, @Nullable String details) {
    Preconditions.checkNotNull(plugin);
    Preconditions.checkNotNull(ideVersion);
    myPlugin = plugin;
    myIdeVersion = ideVersion;
    myDetails = details;
  }

  @NotNull
  @Override
  public String getDescription() {
    return "For " + myPlugin + " there are no updates compatible with " + myIdeVersion + " in the Plugin Repository" + (myDetails != null ? " " + myDetails : "");
  }

  public String getIdeVersion() {
    return myIdeVersion;
  }

  public void setIdeVersion(String ideVersion) {
    myIdeVersion = ideVersion;
  }

  public String getPlugin() {
    return myPlugin;
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
