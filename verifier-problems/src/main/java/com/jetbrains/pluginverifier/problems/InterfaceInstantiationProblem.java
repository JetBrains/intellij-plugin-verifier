package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class InterfaceInstantiationProblem extends Problem {

  private String myClassName;

  public InterfaceInstantiationProblem() {

  }

  public InterfaceInstantiationProblem(@NotNull String className) {
    myClassName = className;
  }

  public String getClassName() {
    return myClassName;
  }

  public void setClassName(String className) {
    myClassName = className;
  }

  @NotNull
  @Override
  public String getDescriptionPrefix() {
    return "instantiation an interface";
  }

  @NotNull
  public String getDescription() {
    return getDescriptionPrefix() + " " + MessageUtils.convertMethodDescr(myClassName);
  }

}
