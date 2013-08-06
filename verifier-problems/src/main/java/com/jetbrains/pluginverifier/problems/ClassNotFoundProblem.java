package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ClassNotFoundProblem extends Problem {

  private String myUnknownClass;

  public ClassNotFoundProblem() {

  }

  public ClassNotFoundProblem(@NotNull String className, @NotNull String methodDescr, String unknownClass) {
    setLocation(new ProblemLocation(className, methodDescr));
    myUnknownClass = unknownClass;
  }

  public String getUnknownClass() {
    return myUnknownClass;
  }

  public void setUnknownClass(String unknownClass) {
    myUnknownClass = unknownClass;
  }

  @Override
  public String getDescription() {
    return "accessing to unknown class: " + MessageUtils.convertClassName(myUnknownClass) + " (from " + getLocation() + ')';
  }

}
