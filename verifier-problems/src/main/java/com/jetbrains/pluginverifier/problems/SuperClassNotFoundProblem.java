package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SuperClassNotFoundProblem extends Problem {

  private String mySuperClassName;

  public SuperClassNotFoundProblem() {

  }

  public SuperClassNotFoundProblem(@NotNull String className, @NotNull String superClassName) {
    setLocation(new ProblemLocation(className));
    mySuperClassName = superClassName;
  }

  @Override
  public String getDescription() {
    return "class has unknown super: " + MessageUtils.convertClassName(mySuperClassName) + " class:" + getLocation();
  }

  public String getSuperClassName() {
    return mySuperClassName;
  }

  public void setSuperClassName(String superClassName) {
    mySuperClassName = superClassName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SuperClassNotFoundProblem)) return false;
    if (!super.equals(o)) return false;

    SuperClassNotFoundProblem problem = (SuperClassNotFoundProblem)o;

    if (mySuperClassName != null ? !mySuperClassName.equals(problem.mySuperClassName) : problem.mySuperClassName != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (mySuperClassName != null ? mySuperClassName.hashCode() : 0);
    return result;
  }
}
