package com.jetbrains.pluginverifier.problems;

import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlTransient;

/**
 * @author Sergey Evdokimov
 */
public abstract class Problem {

  private ProblemLocation myLocation;

  @XmlTransient
  public abstract String getDescription();

  public ProblemLocation getLocation() {
    return myLocation;
  }

  public void setLocation(ProblemLocation location) {
    myLocation = location;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || (o.getClass() != getClass())) return false;

    Problem problem = (Problem)o;

    if (myLocation != null ? !myLocation.equals(problem.myLocation) : problem.myLocation != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myLocation != null ? myLocation.hashCode() : 0;
  }
}
