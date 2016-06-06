package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.persistence.Jsonable;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlTransient;

/**
 * Problem container class.
 */
public abstract class Problem implements Jsonable<Problem> {

  @XmlTransient
  @NotNull
  //TODO: write a renderer for TC and others instead of prefix
  @Deprecated
  public abstract String getDescriptionPrefix();


  @XmlTransient
  @NotNull
  public abstract String getDescription();

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || o.getClass() != getClass()) return false;
    return getDescription().equals(((Problem) o).getDescription());
  }

  @Override
  public final int hashCode() {
    return getDescription().hashCode();
  }

  @Override
  public final String toString() {
    return getDescription();
  }
}
