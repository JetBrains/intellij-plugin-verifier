package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.Pair;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.List;

@XmlRootElement
public class BrokenPluginProblem extends Problem {

  private String myDetails;

  public BrokenPluginProblem() {

  }

  public BrokenPluginProblem(@NotNull String details) {
    myDetails = details;
  }

  @NotNull
  @Override
  public String getDescriptionPrefix() {
    return "broken plugin";
  }

  @NotNull
  @Override
  public String getDescription() {
    return getDescriptionPrefix() + (myDetails != null ? " " + myDetails : "");
  }


  @Override
  public Problem deserialize(String... params) {
    return new BrokenPluginProblem(params[0]);
  }

  @Override
  public List<Pair<String, String>> serialize() {
    return Collections.singletonList(Pair.create("details", myDetails));
  }


}
