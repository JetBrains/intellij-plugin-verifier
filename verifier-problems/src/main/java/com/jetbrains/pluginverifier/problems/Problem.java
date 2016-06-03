package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.Pair;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlTransient;
import java.util.List;

/**
 * Problem container class.
 * Subclasses must have the default public constructor.
 */
public abstract class Problem {

  @XmlTransient
  @NotNull
  //TODO: write a renderer for TC and others instead of prefix
  @Deprecated
  public abstract String getDescriptionPrefix();


  @XmlTransient
  @NotNull
  public abstract String getDescription();

  /**
   * <p>
   * Override this method to deserialize the Problem class.
   * <p>
   * {@code params} are going in the order previously specified with {@link #serialize()}
   * <p>
   * Note: subclasses have to make an effort of deserializing its fields.
   *
   * @param params parameters for the construction of the derived Problem class.
   * @return Problem instance
   */
  public abstract Problem deserialize(String... params);

  /**
   * <p>
   * Override this method to serialize the Problem class.
   * <p>
   * It must return (key, value) pairs of the Problem content.
   * The key represents the key name of the underlying Problem field. The value represents its value.
   * <p>
   * The {@link #deserialize(String...)} method will be called with the parameters
   * according to the List order.
   * <p>
   * Note: subclasses have to make an effort of serialising its fields.
   *
   * @return (key, value) pairs of the Problem content
   */
  public abstract List<Pair<String, String>> serialize();

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
