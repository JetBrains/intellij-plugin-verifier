package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MethodNotFoundProblem extends Problem {

  private String myMethod;

  public MethodNotFoundProblem() {

  }

  public MethodNotFoundProblem(@NotNull String method) {
    myMethod = method;
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
  @Override
  public String getDescriptionPrefix() {
    return "invoking unknown method";
  }

  @NotNull
  public String getDescription() {
    return getDescriptionPrefix() + " " + MessageUtils.convertMethodDescr(myMethod);
  }

}
