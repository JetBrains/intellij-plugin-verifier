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
}
