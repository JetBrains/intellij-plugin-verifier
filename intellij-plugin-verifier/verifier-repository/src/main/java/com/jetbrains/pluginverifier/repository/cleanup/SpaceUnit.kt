/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.cleanup

enum class SpaceUnit(val symbol: String) {
  BYTE("B") {
    override fun toBytes(amount: Long): Double = amount.toDouble()

    override fun toKilobytes(amount: Long): Double = amount / C

    override fun toMegabytes(amount: Long): Double = amount / C2

    override fun toGigabytes(amount: Long): Double = amount / C3
  },
  KILO_BYTE("KB") {
    override fun toBytes(amount: Long): Double = amount * C

    override fun toKilobytes(amount: Long): Double = amount.toDouble()

    override fun toMegabytes(amount: Long): Double = amount / C

    override fun toGigabytes(amount: Long): Double = amount / C2
  },
  MEGA_BYTE("MB") {
    override fun toBytes(amount: Long): Double = amount * C2

    override fun toKilobytes(amount: Long): Double = amount * C

    override fun toMegabytes(amount: Long): Double = amount.toDouble()

    override fun toGigabytes(amount: Long): Double = amount / C
  },
  GIGO_BYTE("GB") {
    override fun toBytes(amount: Long): Double = amount * C3

    override fun toKilobytes(amount: Long): Double = amount * C2

    override fun toMegabytes(amount: Long): Double = amount * C

    override fun toGigabytes(amount: Long): Double = amount.toDouble()
  };

  abstract fun toBytes(amount: Long): Double

  abstract fun toKilobytes(amount: Long): Double

  abstract fun toMegabytes(amount: Long): Double

  abstract fun toGigabytes(amount: Long): Double

  fun to(amount: Long, spaceUnit: SpaceUnit): Double = when (spaceUnit) {
    SpaceUnit.BYTE -> toBytes(amount)
    SpaceUnit.KILO_BYTE -> toKilobytes(amount)
    SpaceUnit.MEGA_BYTE -> toMegabytes(amount)
    SpaceUnit.GIGO_BYTE -> toGigabytes(amount)
  }

  private companion object {
    val C = 1024.0
    val C2 = C * C
    val C3 = C * C * C
  }

  override fun toString() = symbol

}
