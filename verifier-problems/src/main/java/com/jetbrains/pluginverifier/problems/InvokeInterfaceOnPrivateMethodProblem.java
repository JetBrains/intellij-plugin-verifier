package com.jetbrains.pluginverifier.problems;

import com.google.common.base.Preconditions;
import kotlin.Pair;
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


  @NotNull
  @Override
  public Problem deserialize(@NotNull String... params) {
    return new InvokeInterfaceOnPrivateMethodProblem(params[0]);
  }

  @NotNull
  @Override
  public List<Pair<String, String>> serialize() {
    return Collections.singletonList(new Pair<String, String>("method", myMethod));
  }
}
