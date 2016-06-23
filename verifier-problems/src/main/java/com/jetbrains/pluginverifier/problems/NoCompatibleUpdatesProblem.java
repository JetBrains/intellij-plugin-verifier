package com.jetbrains.pluginverifier.problems;


import com.google.common.base.Preconditions;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;
import java.util.List;

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

  public void setPlugin(String plugin) {
    myPlugin = plugin;
  }

  public String getDetails() {
    return myDetails;
  }

  public void setDetails(String details) {
    myDetails = details;
  }


  @NotNull
  @Override
  public Problem deserialize(@NotNull String... params) {
    return new NoCompatibleUpdatesProblem(params[0], params[1], params[2]);
  }

  @NotNull
  @Override
  public List<Pair<String, String>> serialize() {
    //noinspection unchecked
    return Arrays.asList(new Pair<String, String>("plugin", myPlugin), new Pair<String, String>("ideVersion", myIdeVersion), new Pair<String, String>("details", myDetails));
  }


}
