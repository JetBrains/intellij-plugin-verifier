package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;
import java.util.List;

@XmlRootElement
public class MissingDependencyProblem extends Problem {

  private String myPlugin;
  private String myMissingId;
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

  @NotNull
  @Override
  public Problem deserialize(@NotNull String... params) {
    return new MissingDependencyProblem(params[0], params[1], params[2]);
  }

  @NotNull
  @Override
  public List<Pair<String, String>> serialize() {
    //noinspection unchecked
    return Arrays.asList(new Pair<String, String>("plugin", myPlugin), new Pair<String, String>("missingId", myMissingId), new Pair<String, String>("description", myMissDescription));
  }

}
