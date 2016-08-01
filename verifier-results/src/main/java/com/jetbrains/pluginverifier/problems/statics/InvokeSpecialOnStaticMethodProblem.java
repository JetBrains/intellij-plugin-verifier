package com.jetbrains.pluginverifier.problems.statics;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.jetbrains.pluginverifier.problems.Problem;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Sergey Patrikeev
 */

public class InvokeSpecialOnStaticMethodProblem extends Problem {

  @SerializedName("method")
  private String myMethod;

  public InvokeSpecialOnStaticMethodProblem() {
  }

  public InvokeSpecialOnStaticMethodProblem(@NotNull String method) {
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
  @Override
  public String getDescription() {
    return "attempt to perform 'invokespecial' on static method" + " " + myMethod;
  }

}
