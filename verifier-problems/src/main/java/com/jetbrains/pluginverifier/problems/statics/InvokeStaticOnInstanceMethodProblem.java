package com.jetbrains.pluginverifier.problems.statics;

import com.google.common.base.Preconditions;
import com.jetbrains.pluginverifier.problems.Problem;
import kotlin.Pair;
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

  public InvokeStaticOnInstanceMethodProblem(@NotNull String method) {
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
    return "attempt to perform 'invokestatic' on an instance method" + " " + myMethod;
  }


  @NotNull
  @Override
  public Problem deserialize(@NotNull String... params) {
    return new InvokeStaticOnInstanceMethodProblem(params[0]);
  }

  @NotNull
  @Override
  public List<Pair<String, String>> serialize() {
    return Collections.singletonList(new Pair<String, String>("method", myMethod));
  }



}
