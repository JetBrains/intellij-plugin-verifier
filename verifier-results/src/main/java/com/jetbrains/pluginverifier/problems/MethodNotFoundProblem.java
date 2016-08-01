package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;


public class MethodNotFoundProblem extends Problem {

  @SerializedName("method")
  private String myMethod;

  public MethodNotFoundProblem() {

  }

  public MethodNotFoundProblem(@NotNull String method) {
    Preconditions.checkNotNull(method);
    myMethod = method;
  }

  public String getCalledMethod() {
    return myMethod;
  }

  public void setCalledMethod(String calledMethod) {
    //for legacy deserialization (don't add getCalledMethod!)
    myMethod = calledMethod;
  }

  public void setMethod(String method) {
    //for legacy serialization (don't add getMethod)
    myMethod = method;
  }

  //serialized form of the MethodNotFoundProblem will contain "methodDescriptor"
  public String getMethodDescriptor() {
    return myMethod;
  }

  public void setMethodDescriptor(String methodDescriptor) {
    myMethod = methodDescriptor;
  }

  @NotNull
  public String getDescription() {
    return "invoking unknown method" + " " + MessageUtils.INSTANCE.convertMethodDescr(myMethod);
  }

}
