package com.jetbrains.plugin.structure.classes.utils

@Suppress("unused")
internal class MockInternalClass {
  private val privateField = "private"

  internal val internalField = "internal"

  internal fun internalMethod(s: String, i: Int) = "internal"
  fun internalMethod(s: String) = "internal"
}