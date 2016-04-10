package com.jetbrains.pluginverifier.problems;

import javax.xml.bind.annotation.XmlTransient;

/**
 * @author Sergey Evdokimov
 */
public abstract class Problem {

  @XmlTransient
  //TODO: write a renderer for TC and others instead of prefix
  public abstract String getDescriptionPrefix();


  @XmlTransient
  public abstract String getDescription();

  @Override
  public boolean equals(Object o) {
    throw new UnsupportedOperationException("Children of com.jetbrains.pluginverifier.problems.Problem must override equals() and hashcode()");
  }

  @Override
  public int hashCode() {
    throw new UnsupportedOperationException("Children of com.jetbrains.pluginverifier.problems.Problem must override equals() and hashcode()");
  }

  @Override
  public String toString() {
    return getDescription();
  }
}
