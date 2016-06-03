package com.jetbrains.pluginverifier.problems.statics;

import com.jetbrains.pluginverifier.problems.Problem;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Sergey Patrikeev
 */
@XmlRootElement
public class InvokeVirtualOnStaticMethodProblem extends Problem {

  private String myMethod;

  public InvokeVirtualOnStaticMethodProblem() {
  }

  public InvokeVirtualOnStaticMethodProblem(String method) {
    myMethod = method;
  }

  @NotNull
  @Override
  public String getDescriptionPrefix() {
    return "attempt to perform 'invokevirtual' on static method";
  }

  public String getMethod() {
    return myMethod;
  }

  public void setMethod(String method) {
    myMethod = method;
  }

  @NotNull
  @Override
  public String getDescription() {
    return getDescriptionPrefix() + " " + myMethod;
  }

}
