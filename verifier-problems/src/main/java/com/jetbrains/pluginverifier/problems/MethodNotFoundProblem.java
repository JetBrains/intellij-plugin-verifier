package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MethodNotFoundProblem extends MethodProblem {

  private String myCalledMethod;

  public MethodNotFoundProblem() {

  }

  public MethodNotFoundProblem(@NotNull String className, @NotNull String methodDescr, @NotNull String calledMethod) {
    super(className, methodDescr);
    myCalledMethod = calledMethod;
  }

  public String getCalledMethod() {
    return myCalledMethod;
  }

  public void setCalledMethod(String calledMethod) {
    myCalledMethod = calledMethod;
    cleanUid();
  }

  @Override
  public String getDescription() {
    return "invoking unknown method: " + MessageUtils.convertMethodDescr(myCalledMethod) + " (from " + getMethodDescrHuman() + ')';
  }

  @NotNull
  @Override
  public String evaluateUID() {
    return evaluateUID(myCalledMethod, getMethodDescr());
  }
}
