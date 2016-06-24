package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Sergey Patrikeev
 */
@XmlRootElement
public class InvokeInterfaceOnPrivateMethodProblem extends Problem {

  @SerializedName("method")
  private String myMethod;

  public InvokeInterfaceOnPrivateMethodProblem() {
  }

  public InvokeInterfaceOnPrivateMethodProblem(String method) {
    Preconditions.checkNotNull(method);
    myMethod = method;
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
    return "attempt to perform 'invokeinterface' on private method" + " " + myMethod;
  }

}
