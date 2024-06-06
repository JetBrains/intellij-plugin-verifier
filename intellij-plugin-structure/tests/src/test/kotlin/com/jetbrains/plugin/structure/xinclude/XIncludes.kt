package com.jetbrains.plugin.structure.xinclude

import com.jetbrains.plugin.structure.intellij.xinclude.IS_RESOLVING_CONDITIONAL_INCLUDES_PROPERTY

internal fun withSystemProperty(property: String, value: Boolean, block: () -> Unit) {
  try {
    System.setProperty(property, value.toString())
    block()
  } finally {
    System.clearProperty(property)
  }
}

internal fun withConditionalXIncludes(block: () -> Unit) {
  withSystemProperty(IS_RESOLVING_CONDITIONAL_INCLUDES_PROPERTY, true) {
    block()
  }
}