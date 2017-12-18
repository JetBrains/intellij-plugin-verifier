package org.jetbrains.plugins.verifier.service.database

import org.mapdb.Serializer

/**
 * Represents a type of the value stored in the [database] [ServerDatabase].
 */
sealed class ValueType<T> {

  object STRING : ValueType<String>() {
    override val serializer: Serializer<String> = Serializer.STRING
  }

  object INT : ValueType<Int>() {
    override val serializer: Serializer<Int> = Serializer.INTEGER
  }

  /**
   * Type of all the [Java Serializable] [java.io.Serializable] values.
   */
  object SERIALIZABLE : ValueType<Any>() {
    override val serializer: Serializer<Any> = Serializer.JAVA
  }

  abstract val serializer: Serializer<T>

}