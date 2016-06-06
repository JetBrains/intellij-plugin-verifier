package com.jetbrains.pluginverifier.persistence;

import com.jetbrains.pluginverifier.utils.Pair;

import java.util.List;

/**
 * <p>
 * Implement this interface to make your subclass serializable/deserializable to and from Json.
 * <p>
 * Subclasses <strong>must have</strong> the default public constructor.
 *
 * @author Sergey Patrikeev
 */
public interface Jsonable<T> {

  /**
   * <p>
   * Override this method to serialize the subclass
   * <p>
   * It must return (key, value) pairs of the subclass content.
   * The key represents the key name of the underlying field. The value represents its value.
   * <p>
   * The {@link #deserialize(String...)} method will be called with the parameters
   * according to the List order.
   * <p>
   * Note: subclasses have to make an effort of serialising its fields.
   *
   * @return (key, value) pairs of the content
   */
  List<Pair<String, String>> serialize();

  /**
   * <p>
   * Override this method to deserialize your subclass
   * <p>
   * {@code params} are going in the order previously specified with {@link #serialize()}
   * <p>
   * Note: subclasses have to make an effort of deserializing its fields.
   *
   * @param params parameters for the construction of the subclass.
   * @return subclass instance
   */
  T deserialize(String... params);

}
