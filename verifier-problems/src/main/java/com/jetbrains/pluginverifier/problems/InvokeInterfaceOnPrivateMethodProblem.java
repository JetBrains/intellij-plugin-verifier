package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import com.jetbrains.pluginverifier.utils.Pair;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.List;

/**
 * Created by Sergey Patrikeev
 */
@XmlRootElement
public class InvokeInterfaceOnPrivateMethodProblem extends Problem {

  private String myMethod;

  public InvokeInterfaceOnPrivateMethodProblem() {
  }

  public InvokeInterfaceOnPrivateMethodProblem(String method) {
    Preconditions.checkNotNull(method);
    myMethod = method;
  }

  @NotNull
  @Override
  public String getDescriptionPrefix() {
    return "attempt to perform 'invokeinterface' on private method";
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


  @Override
  public Problem deserialize(String... params) {
    return new InvokeInterfaceOnPrivateMethodProblem(params[0]);
  }

  @Override
  public List<Pair<String, String>> serialize() {
    return Collections.singletonList(Pair.create("method", myMethod));
  }
}
