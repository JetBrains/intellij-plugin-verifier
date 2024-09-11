package com.jetbrains.pluginverifier.tests.bytecode

import com.jetbrains.pluginverifier.verifiers.resolution.BinaryClassName

internal data class BinaryClassContent(val name: BinaryClassName, val bytes: ByteArray) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as BinaryClassContent

    if (name != other.name) return false
    if (!bytes.contentEquals(other.bytes)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = name.hashCode()
    result = 31 * result + bytes.contentHashCode()
    return result
  }
}
