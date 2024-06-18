package com.jetbrains.plugin.structure.classes.utils

internal class MockInternalClass {
  private val privateField = "private"

  internal val internalField = "internal"

  internal fun internalMethod(s: String, i: Int) = "internal"
  fun internalMethod(s: String) = "internal"
}