package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class AbstractClassInstantiationProblem extends Problem {

  private String myClassName;

  public AbstractClassInstantiationProblem() {

  }

  public AbstractClassInstantiationProblem(@NotNull String className) {
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
    return "instantiation an abstract class";
  }

  @NotNull
  public String getDescription() {
    return getDescriptionPrefix() + " " + MessageUtils.convertMethodDescr(myClassName);
  }

}