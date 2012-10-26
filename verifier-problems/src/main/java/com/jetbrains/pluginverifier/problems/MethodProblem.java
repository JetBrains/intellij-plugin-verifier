package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlTransient;

/**
 * @author Sergey Evdokimov
 */
public abstract class MethodProblem extends Problem {

  private String myMethodDescr;

  public MethodProblem() {

  }

  public MethodProblem(@NotNull String className, @NotNull String methodDescr) {
    myMethodDescr = className + '#' + methodDescr;
  }

  public String getMethodDescr() {
    return myMethodDescr;
  }

  @XmlTransient
  protected String getMethodDescrHuman() {
    return MessageUtils.convertMethodDescr(myMethodDescr);
  }

  public void setMethodDescr(String methodDescr) {
    myMethodDescr = methodDescr;
    cleanUid();
  }
}
