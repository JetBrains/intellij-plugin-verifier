package com.jetbrains.plugin.structure.intellij.plugin

sealed class KotlinPluginMode(
  @Suppress("unused") val isK1Compatible: Boolean,
  @Suppress("unused") val isK2Compatible: Boolean
) {
  object K1AndK2Compatible : KotlinPluginMode(isK1Compatible = true, isK2Compatible = true) {
    override fun toString() = "K1 and K2 compatible"
  }

  object K2OnlyCompatible : KotlinPluginMode(isK1Compatible = false, isK2Compatible = true) {
    override fun toString() = "K2-only compatible"
  }

  object K1OnlyCompatible : KotlinPluginMode(isK1Compatible = true, isK2Compatible = false) {
    override fun toString() = "K1-only compatible"
  }

  object Implicit : KotlinPluginMode(isK1Compatible = true, isK2Compatible = false) {
    override fun toString() = "Implicit compatibility mode (K1-only)"
  }

  companion object {
    fun parse(isK1Compatible: Boolean, isK2Compatible: Boolean): KotlinPluginMode =
      if (isK1Compatible) {
        if (isK2Compatible) K1AndK2Compatible else K1OnlyCompatible
      } else {
        K2OnlyCompatible
      }
  }
}