package com.jetbrains.plugin.structure.intellij.utils

enum class ThreeState {
  YES, NO, UNSURE;

  fun toBoolean() = when (this) {
    ThreeState.YES -> true
    ThreeState.NO -> false
    ThreeState.UNSURE -> throw IllegalStateException("Must be or YES, or NO")
  }

  companion object {
    @JvmStatic
    fun fromBoolean(value: Boolean) = if (value) YES else NO
  }
}