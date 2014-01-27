package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class OverridingFinalMethodProblem extends Problem {

  private String mySuperFinalMethod;

  public OverridingFinalMethodProblem() {

  }

  public OverridingFinalMethodProblem(@NotNull String className, @NotNull String method, @NotNull String superFinalMethod) {
    setLocation(new ProblemLocation(className, method));
    mySuperFinalMethod = superFinalMethod;
  }

  public String getMethod() {
    return mySuperFinalMethod;
  }

  public void setMethod(String method) {
    mySuperFinalMethod = method;
  }

  @Override
  public String getDescription() {
    return "overriding final method: " + MessageUtils.convertMethodDescr(mySuperFinalMethod);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof OverridingFinalMethodProblem)) return false;
    if (!super.equals(o)) return false;

    OverridingFinalMethodProblem problem = (OverridingFinalMethodProblem)o;

    if (mySuperFinalMethod != null ? !mySuperFinalMethod.equals(problem.mySuperFinalMethod) : problem.mySuperFinalMethod != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (mySuperFinalMethod != null ? mySuperFinalMethod.hashCode() : 0);
    return result;
  }
}
