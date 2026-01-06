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
package com.jetbrains.pluginverifier.plugin

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
internal abstract class Striped<L : Any> private constructor() {
  /**
   * Returns the stripe that corresponds to the passed key. It is always guaranteed that if `key1.equals(key2)`, then `get(key1) == get(key2)`.
   * 
   * @param key an arbitrary, non-null key
   * @return the stripe that the passed key corresponds to
   */
  abstract fun get(key: Any): L

  /**
   * Returns the stripe at the specified index. Valid indexes are 0, inclusively, to `size()`,
   * exclusively.
   * 
   * @param index the index of the stripe to return; must be in `[0...size())`
   * @return the stripe at the specified index
   */
  abstract fun getAt(index: Int): L

  /**
   * Returns the index to which the given key is mapped, so that getAt(indexFor(key)) == get(key).
   */
  abstract fun indexFor(key: Any): Int

  /**
   * Returns the total number of stripes in this instance.
   */
  abstract fun size(): Int

  private abstract class PowerOfTwoStriped<L : Any>(stripes: Int) : Striped<L>() {
    /**
     * Capacity (power of two) minus one, for fast mod evaluation
     */
    val mask: Int

    init {
      require(stripes > 0) { "Stripes must be positive" }
      this.mask = if (stripes > MAX_POWER_OF_TWO) ALL_SET else ceilToPowerOfTwo(stripes) - 1
    }

    override fun indexFor(key: Any): Int {
      val hash: Int = smear(key.hashCode())
      return hash and mask
    }

    override fun get(key: Any): L {
      return getAt(indexFor(key))
    }
  }

  /**
   * Implementation of Striped where 2^k stripes are represented as an array of the same length,
   * eagerly initialized.
   */
  private class CompactStriped<L : Any>(stripes: Int, supplier: Supplier<L>) : PowerOfTwoStriped<L>(stripes) {
    /**
     * Size is a power of two.
     */
    private val array: Array<Any>

    init {
      require(stripes <= MAX_POWER_OF_TWO) { "Stripes must be <= 2^30)" }
      @Suppress("UNCHECKED_CAST")
      this.array = Array(mask + 1, init = { supplier.get() as Any })
    }

    override fun getAt(index: Int): L {
      @Suppress("UNCHECKED_CAST")
      return array[index] as L
    }

    override fun size(): Int {
      return array.size
    }
  }

  @Suppress("unused")
  private class PaddedLock : ReentrantLock(false) {
    /*
     * Padding from 40 into 64 bytes, same size as cache line. Might be beneficial to add a fourth
     * long here, to minimize the chance of interference between consecutive locks, but I couldn't
     * observe any benefit from that.
     */
    var unused1: Long = 0
    var unused2: Long = 0
    var unused3: Long = 0
  }

  companion object {
    private const val MAX_POWER_OF_TWO = 1 shl (Integer.SIZE - 2)

    // Static factories
    /**
     * Creates a `Striped<L>` with eagerly initialized, strongly referenced locks. Every lock is
     * obtained from the passed supplier.
     * 
     * @param stripes  the minimum number of stripes (locks) required
     * @param supplier a `Supplier<L>` object to get locks from
     * @return a new `Striped<L>`
     * @since 33.5.0
     */
    fun <L : Any> custom(stripes: Int, supplier: Supplier<L>): Striped<L> {
      return CompactStriped(stripes, supplier)
    }

    /**
     * Creates a `Striped<Lock>` with eagerly initialized, strongly referenced locks. Every lock
     * is reentrant.
     * 
     * @param stripes the minimum number of stripes (locks) required
     * @return a new `Striped<Lock>`
     */
    fun lock(stripes: Int): Striped<Lock> {
      return custom(stripes) { PaddedLock() }
    }


    /**
     * A bit mask where all bits are set.
     */
    private const val ALL_SET = 0.inv()

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
