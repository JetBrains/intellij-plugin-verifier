/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.database

import org.mapdb.DataInput2
import org.mapdb.DataOutput2
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

  /**
   * [ValueType] of any value that can be converted
   * to and obtained from a string.
   */
  class StringBased<T>(
    val toString: (T) -> String,
    val fromString: (String) -> T
  ) : ValueType<T>() {

    override val serializer = object : Serializer<T> {
      override fun serialize(out: DataOutput2, value: T) {
        Serializer.STRING.serialize(out, toString(value))
      }

      override fun deserialize(input: DataInput2, available: Int) =
        fromString(Serializer.STRING.deserialize(input, available))
    }
  }

  abstract val serializer: Serializer<T>

}