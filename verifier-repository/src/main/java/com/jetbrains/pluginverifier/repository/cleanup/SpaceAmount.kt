package com.jetbrains.pluginverifier.repository.cleanup

import org.apache.commons.io.FileUtils
import java.io.File

data class SpaceAmount(private val bytes: Long) : Comparable<SpaceAmount> {
  companion object {
    fun ofBytes(bytes: Long): SpaceAmount = SpaceAmount(bytes)

    fun ofKilobytes(kilos: Long) = SpaceAmount(SpaceUnit.KILO_BYTE.toBytes(kilos).toLong())

    fun ofMegabytes(megabytes: Long) = SpaceAmount(SpaceUnit.MEGA_BYTE.toBytes(megabytes).toLong())

    fun ofGigabytes(gigabytes: Long) = SpaceAmount(SpaceUnit.GIGO_BYTE.toBytes(gigabytes).toLong())

    val ONE_KILO_BYTE = ofKilobytes(1)

    val ONE_MEGA_BYTE = ofMegabytes(1)

    val ONE_GIGO_BYTE = ofGigabytes(1)

    val ZERO_SPACE = ofBytes(0)
  }

  operator fun times(multiplier: Double) =
      SpaceAmount((bytes * multiplier).toLong())

  operator fun times(multiplier: Long) =
      SpaceAmount(bytes * multiplier)

  operator fun plus(otherAmount: SpaceAmount) =
      SpaceAmount(bytes + otherAmount.bytes)

  operator fun minus(otherAmount: SpaceAmount) =
      SpaceAmount(bytes - otherAmount.bytes)

  operator fun div(divisor: Double) =
      SpaceAmount((bytes / divisor).toLong())

  override fun compareTo(other: SpaceAmount) =
      bytes.compareTo(other.bytes)

  fun to(spaceUnit: SpaceUnit) = SpaceUnit.BYTE.to(bytes, spaceUnit)

  fun presentableAmount(): String {
    val preferredUnit = when {
      this < ONE_KILO_BYTE -> SpaceUnit.BYTE
      this < ONE_MEGA_BYTE -> SpaceUnit.KILO_BYTE
      this < ONE_GIGO_BYTE -> SpaceUnit.MEGA_BYTE
      else -> SpaceUnit.GIGO_BYTE
    }
    val targetAmount = SpaceUnit.BYTE.to(bytes, preferredUnit)
    if (targetAmount == targetAmount.toLong().toDouble()) {
      return "${targetAmount.toLong()} $preferredUnit"
    }
    return "%.2f %s".format(targetAmount, preferredUnit)
  }

  override fun toString(): String = presentableAmount()

}

fun Long.bytesToSpaceAmount() = SpaceAmount.ofBytes(this)

val File.fileSize: SpaceAmount
  get() = FileUtils.sizeOf(this).bytesToSpaceAmount()