package com.jetbrains.pluginverifier.verifiers.bytecode

import org.objectweb.asm.Type
import org.objectweb.asm.tree.analysis.BasicValue

data class StringValue(val value: String) : BasicValue(Type.getType(String::class.java)) {
  override fun toString(): String = "\"$value\""
}
data class IntValue(val value: Int) : BasicValue(Type.INT_TYPE) {
  override fun toString(): String = value.toString()
}