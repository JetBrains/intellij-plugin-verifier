package com.jetbrains.pluginverifier.persistence

/**
 * Implement this interface to make your subclass serializable/deserializable to and from Json.
 *
 * Subclasses **must have** the default public constructor.
 * @author Sergey Patrikeev
 */
interface Jsonable<out T> {

  /**
   * Override this method to serialize the subclass
   *
   * It must return (key, value) pairs of the subclass content.
   * The key represents the key name of the underlying field. The value represents its value.
   *
   * The [deserialize] method will be called with the parameters
   * according to the List order.
   *
   * Note: subclasses have to make an effort of serialising its fields.
   * @return (key, value) pairs of the content
   */
  fun serialize(): List<Pair<String, String?>>

  /**
   * Override this method to deserialize your subclass
   * `params` are going in the order previously specified with [serialize]
   *
   * Note: subclasses have to make an effort of deserializing its fields.

   * @param params parameters for the construction of the subclass.
   * @return subclass instance
   */
  fun deserialize(vararg params: String?): T

}
