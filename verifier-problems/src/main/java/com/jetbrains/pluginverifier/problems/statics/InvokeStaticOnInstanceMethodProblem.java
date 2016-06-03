package com.jetbrains.pluginverifier.problems.statics;

import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.utils.Pair;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.List;

/**
 * Created by Sergey Patrikeev
 */
@XmlRootElement
public class InvokeStaticOnInstanceMethodProblem extends Problem {

  private String myMethod;

  public InvokeStaticOnInstanceMethodProblem() {
  }

  public InvokeStaticOnInstanceMethodProblem(String method) {
    myMethod = method;
  }

  @NotNull
  @Override
  public String getDescriptionPrefix() {
    return "attempt to perform 'invokestatic' on an instance method";
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
    return new InvokeStaticOnInstanceMethodProblem(params[0]);
  }

  @Override
  public List<Pair<String, String>> serialize() {
    return Collections.singletonList(Pair.create("method", myMethod));
  }



}
