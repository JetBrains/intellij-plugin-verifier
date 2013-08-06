package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class OverridingFinalMethodProblem extends Problem {

  private String myMethod;

  public OverridingFinalMethodProblem() {

  }

  public OverridingFinalMethodProblem(@NotNull String method) {
    setLocation(new ProblemLocation());
    myMethod = method;
  }

  public String getMethod() {
    return myMethod;
  }

  public void setMethod(String method) {
    myMethod = method;
  }

  @Override
  public String getDescription() {
    return "overriding final method: " + MessageUtils.convertMethodDescr(myMethod);
  }

}
