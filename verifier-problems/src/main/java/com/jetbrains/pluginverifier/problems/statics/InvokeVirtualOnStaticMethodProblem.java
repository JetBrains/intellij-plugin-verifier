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


  @Override
  public Problem deserialize(String... params) {
    return new InvokeVirtualOnStaticMethodProblem(params[0]);
  }

  @Override
  public List<Pair<String, String>> serialize() {
    return Collections.singletonList(Pair.create("method", myMethod));
  }

}
