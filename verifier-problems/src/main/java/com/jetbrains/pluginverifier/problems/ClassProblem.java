package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlTransient;

/**
 * @author Sergey Evdokimov
 */
public abstract class ClassProblem extends Problem {

  private String myClassName;

  public ClassProblem() {

  }

  public ClassProblem(@NotNull String className) {
    myClassName = className;
  }

  public String getClassName() {
    return myClassName;
  }

  @XmlTransient
  protected String getClassNameHuman() {
    return MessageUtils.convertClassName(myClassName);
  }

  public void setClassName(String className) {
    myClassName = className;
    cleanUid();
  }
}
