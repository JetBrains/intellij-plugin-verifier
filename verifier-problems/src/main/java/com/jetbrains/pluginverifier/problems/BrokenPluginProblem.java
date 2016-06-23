package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import kotlin.Pair;
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
    Preconditions.checkNotNull(details);
    myDetails = details;
  }

  @NotNull
  @Override
  public String getDescription() {
    return "broken plugin" + (myDetails != null ? " " + myDetails : "");
  }


  @NotNull
  @Override
  public Problem deserialize(@NotNull String... params) {
    return new BrokenPluginProblem(params[0]);
  }

  @NotNull
  @Override
  public List<Pair<String, String>> serialize() {
    return Collections.singletonList(new Pair<String, String>("details", myDetails));
  }


}
