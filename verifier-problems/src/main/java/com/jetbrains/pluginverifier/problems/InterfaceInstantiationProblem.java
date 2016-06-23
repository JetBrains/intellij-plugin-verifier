package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import com.jetbrains.pluginverifier.utils.MessageUtils;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.List;

@XmlRootElement
public class InterfaceInstantiationProblem extends Problem {

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


  @NotNull
  @Override
  public Problem deserialize(@NotNull String... params) {
    return new InterfaceInstantiationProblem(params[0]);
  }

  @NotNull
  @Override
  public List<Pair<String, String>> serialize() {
    return Collections.singletonList(new Pair<String, String>("class", myClassName));
  }

}
