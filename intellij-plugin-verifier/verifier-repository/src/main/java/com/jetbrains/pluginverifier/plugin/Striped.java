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

package com.jetbrains.pluginverifier.plugin;


import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Simplified copy of com.google.common.util.concurrent.Striped
 *
 * @author Dimitris Andreou
 */
abstract class Striped<L> {
  private static final int MAX_POWER_OF_TWO = 1 << (Integer.SIZE - 2);

  private Striped() {
  }

  /**
   * Returns the stripe that corresponds to the passed key. It is always guaranteed that if {@code
   * key1.equals(key2)}, then {@code get(key1) == get(key2)}.
   *
   * @param key an arbitrary, non-null key
   * @return the stripe that the passed key corresponds to
   */
  public abstract L get(Object key);

  /**
   * Returns the stripe at the specified index. Valid indexes are 0, inclusively, to {@code size()},
   * exclusively.
   *
   * @param index the index of the stripe to return; must be in {@code [0...size())}
   * @return the stripe at the specified index
   */
  public abstract L getAt(int index);

  /**
   * Returns the index to which the given key is mapped, so that getAt(indexFor(key)) == get(key).
   */
  abstract int indexFor(Object key);

  /**
   * Returns the total number of stripes in this instance.
   */
  public abstract int size();

  // Static factories

  /**
   * Creates a {@code Striped<L>} with eagerly initialized, strongly referenced locks. Every lock is
   * obtained from the passed supplier.
   *
   * @param stripes  the minimum number of stripes (locks) required
   * @param supplier a {@code Supplier<L>} object to obtain locks from
   * @return a new {@code Striped<L>}
   * @since 33.5.0
   */
  public static <L> Striped<L> custom(int stripes, Supplier<L> supplier) {
    return new CompactStriped<>(stripes, supplier);
  }

  /**
   * Creates a {@code Striped<Lock>} with eagerly initialized, strongly referenced locks. Every lock
   * is reentrant.
   *
   * @param stripes the minimum number of stripes (locks) required
   * @return a new {@code Striped<Lock>}
   */
  public static Striped<Lock> lock(int stripes) {
    return custom(stripes, PaddedLock::new);
  }


  private abstract static class PowerOfTwoStriped<L> extends Striped<L> {
    /**
     * Capacity (power of two) minus one, for fast mod evaluation
     */
    final int mask;

    PowerOfTwoStriped(int stripes) {
      if (stripes <= 0) {
        throw new IllegalArgumentException("Stripes must be positive");
      }
      this.mask = stripes > MAX_POWER_OF_TWO ? ALL_SET : ceilToPowerOfTwo(stripes) - 1;
    }

    @Override
    final int indexFor(Object key) {
      int hash = smear(key.hashCode());
      return hash & mask;
    }

    @Override
    public final L get(Object key) {
      return getAt(indexFor(key));
    }
  }

  /**
   * Implementation of Striped where 2^k stripes are represented as an array of the same length,
   * eagerly initialized.
   */
  private static final class CompactStriped<L> extends PowerOfTwoStriped<L> {
    /**
     * Size is a power of two.
     */
    private final Object[] array;

    private CompactStriped(int stripes, Supplier<L> supplier) {
      super(stripes);
      if (stripes > MAX_POWER_OF_TWO) {
        throw new IllegalArgumentException("Stripes must be <= 2^30)");
      }

      this.array = new Object[mask + 1];
      for (int i = 0; i < array.length; i++) {
        array[i] = supplier.get();
      }
    }

    @SuppressWarnings("unchecked") // we only put L's in the array
    @Override
    public L getAt(int index) {
      return (L) array[index];
    }

    @Override
    public int size() {
      return array.length;
    }
  }

  /**
   * A bit mask were all bits are set.
   */
  private static final int ALL_SET = ~0;

  private static int ceilToPowerOfTwo(int x) {
    if (x <= 0) {
      throw new IllegalArgumentException("x" + " (" + x + ") must be > 0");
    }
    int result = Integer.SIZE - Integer.numberOfLeadingZeros(x - 1);
    return 1 << result;
  }

  /*
   * This method was written by Doug Lea with assistance from members of JCP JSR-166 Expert Group
   * and released to the public domain, as explained at
   * http://creativecommons.org/licenses/publicdomain
   *
   * As of 2010/06/11, this method is identical to the (package private) hash method in OpenJDK 7's
   * java.util.HashMap class.
   */
  // Copied from java/com/google/common/collect/Hashing.java
  private static int smear(int hashCode) {
    hashCode ^= (hashCode >>> 20) ^ (hashCode >>> 12);
    return hashCode ^ (hashCode >>> 7) ^ (hashCode >>> 4);
  }

  @SuppressWarnings("unused")
  private static final class PaddedLock extends ReentrantLock {
    /*
     * Padding from 40 into 64 bytes, same size as cache line. Might be beneficial to add a fourth
     * long here, to minimize chance of interference between consecutive locks, but I couldn't
     * observe any benefit from that.
     */
    long unused1;
    long unused2;
    long unused3;

    PaddedLock() {
      super(false);
    }
  }
}
