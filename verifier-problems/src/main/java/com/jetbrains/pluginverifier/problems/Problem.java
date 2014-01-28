package com.jetbrains.pluginverifier.problems;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlTransient;

/**
 * @author Sergey Evdokimov
 */
public abstract class Problem {

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
}
