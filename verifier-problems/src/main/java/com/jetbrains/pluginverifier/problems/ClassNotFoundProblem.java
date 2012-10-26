package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.problems.MethodProblem;
import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ClassNotFoundProblem extends MethodProblem {

  private String myUnknownClass;

  public ClassNotFoundProblem() {

  }

  public ClassNotFoundProblem(@NotNull String className, @NotNull String methodDescr, String unknownClass) {
    super(className, methodDescr);
    myUnknownClass = unknownClass;
  }

  public String getUnknownClass() {
    return myUnknownClass;
  }

  public void setUnknownClass(String unknownClass) {
    myUnknownClass = unknownClass;
    cleanUid();
  }

  @Override
  public String getDescription() {
    return "accessing to unknown class: " + MessageUtils.convertClassName(myUnknownClass) + " (from " + getMethodDescrHuman() + ')';
  }

  @NotNull
  @Override
  public String evaluateUID() {
    return evaluateUID(myUnknownClass, getMethodDescr());
  }
}
