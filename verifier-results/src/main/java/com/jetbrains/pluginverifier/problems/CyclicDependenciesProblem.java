package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;


public class CyclicDependenciesProblem extends Problem {

  @SerializedName("cycle")
  private String myCycle;

  public CyclicDependenciesProblem() {

  }

  public CyclicDependenciesProblem(@NotNull String cycle) {
    Preconditions.checkNotNull(cycle);
    myCycle = cycle;
  }

  public String getCycle() {
    return myCycle;
  }

  public void setCycle(String cycle) {
    myCycle = cycle;
  }

  @NotNull
  public String getDescription() {
    return "cyclic plugin dependencies" + (myCycle != null ? " " + myCycle : "");
  }


}
