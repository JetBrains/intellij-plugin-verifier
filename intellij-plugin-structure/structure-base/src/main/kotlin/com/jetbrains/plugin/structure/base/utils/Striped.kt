/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jetbrains.plugin.structure.base.utils

import org.jetbrains.annotations.ApiStatus
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Supplier


/**
 * Simplified copy of com.google.common.util.concurrent.Striped
 * 
 * @author Dimitris Andreou
 */
@ApiStatus.Internal
abstract class Striped<L : Any> private constructor() {
  /**
   * Returns the stripe that corresponds to the passed key. It is always guaranteed that if `key1.equals(key2)`, then `get(key1) == get(key2)`.
   * 
   * @param key an arbitrary, non-null key
   * @return the stripe that the passed key corresponds to
   */
  abstract fun get(key: Any): L

  /**
   * Returns the total number of stripes in this instance.
   */
  abstract fun size(): Int

  /**
   * Implementation of Striped where 2^k stripes are represented as an array of the same length,
   * eagerly initialized.
   */
  private class CompactStriped<L : Any>(stripes: Int, supplier: Supplier<L>) : Striped<L>() {
    /**
     * Capacity (power of two) minus one, for fast mod evaluation
     */
    private val mask: Int

    /**
     * Size is a power of two.
     */
    private val array: Array<Any>

    init {
      require(stripes > 0) { "Stripes must be positive" }
      require(stripes <= MAX_POWER_OF_TWO) { "Stripes must be <= 2^30)" }
      mask = ceilToPowerOfTwo(stripes) - 1
      @Suppress("UNCHECKED_CAST")
      array = Array(mask + 1, init = { supplier.get() as Any })
    }

    override fun get(key: Any): L {
      val hash: Int = smear(key.hashCode())
      val index = hash and mask
      @Suppress("UNCHECKED_CAST")
      return array[index] as L
    }

    override fun size(): Int {
      return array.size
    }
  }

  companion object {
    private const val MAX_POWER_OF_TWO = 1 shl (Integer.SIZE - 2)

    /**
     * Creates a `Striped<Lock>` with eagerly initialized, strongly referenced locks. Every lock
     * is reentrant.
     * 
     * @param stripes the minimum number of stripes (locks) required
     * @return a new `Striped<Lock>`
     */
    fun lock(stripes: Int): Striped<Lock> {
      return CompactStriped(stripes) { ReentrantLock() }
    }

    private fun ceilToPowerOfTwo(x: Int): Int {
      require(x > 0) { "x ($x) must be > 0" }
      val result = Integer.SIZE - Integer.numberOfLeadingZeros(x - 1)
      return 1 shl result
    }

    /*
     * This method was written by Doug Lea with assistance from members of JCP JSR-166 Expert Group
     * and released to the public domain, as explained at
     * http://creativecommons.org/licenses/publicdomain
     *
     * As of 2010/06/11, this method is identical to the (package private) hash method in OpenJDK 7's
     * java.util.HashMap class.
     *
     * Copied from java/com/google/common/collect/Hashing.java
     */
    private fun smear(hashCode: Int): Int {
      var hashCode = hashCode
      hashCode = hashCode xor ((hashCode ushr 20) xor (hashCode ushr 12))
      return hashCode xor (hashCode ushr 7) xor (hashCode ushr 4)
    }
  }
}
