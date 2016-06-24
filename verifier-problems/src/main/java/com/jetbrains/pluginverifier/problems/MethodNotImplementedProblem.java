package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MethodNotImplementedProblem extends Problem {

  @SerializedName("method")
  private String myMethod;

  public MethodNotImplementedProblem() {

  }

  public MethodNotImplementedProblem(@NotNull String method) {
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
    return "method isn't implemented" + " " + MessageUtils.convertMethodDescr(myMethod);
  }

}
