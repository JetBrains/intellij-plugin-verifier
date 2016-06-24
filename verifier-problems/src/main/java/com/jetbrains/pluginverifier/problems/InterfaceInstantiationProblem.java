package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class InterfaceInstantiationProblem extends Problem {

  @SerializedName("class")
  private String myClassName;

  public InterfaceInstantiationProblem() {

  }

  public InterfaceInstantiationProblem(@NotNull String className) {
    Preconditions.checkNotNull(className);
    myClassName = className;
  }

  public String getClassName() {
    return myClassName;
  }

  public void setClassName(String className) {
    myClassName = className;
  }

  @NotNull
  public String getDescription() {
    return "instantiation an interface" + " " + MessageUtils.convertClassName(myClassName);
  }


}
