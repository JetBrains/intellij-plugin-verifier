package com.jetbrains.plugin.structure.intellij.utils

enum class ThreeState {
  YES, NO, UNSURE;

  companion object {
    @JvmStatic
    fun fromBoolean(value: Boolean) = if (value) YES else NO
  }
}