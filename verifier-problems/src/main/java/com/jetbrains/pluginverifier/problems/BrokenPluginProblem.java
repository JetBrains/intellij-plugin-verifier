package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class BrokenPluginProblem extends Problem {

  @SerializedName("details")
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


}
