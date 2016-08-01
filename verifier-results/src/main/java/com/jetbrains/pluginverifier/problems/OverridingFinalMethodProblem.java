package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;


public class OverridingFinalMethodProblem extends Problem {

  @SerializedName("method")
  private String myMethod;

  public OverridingFinalMethodProblem() {

  }

  public OverridingFinalMethodProblem(@NotNull String method) {
    Preconditions.checkNotNull(method);
    myMethod = method;
  }

  public String getMethod() {
    return myMethod;
  }

  public void setMethod(String method) {
    myMethod = method;
  }

  @NotNull
  public String getDescription() {
    return "overriding final method" + " " + MessageUtils.INSTANCE.convertMethodDescr(myMethod);
  }

}
