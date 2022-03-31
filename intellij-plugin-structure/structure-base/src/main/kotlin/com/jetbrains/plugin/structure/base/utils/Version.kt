package com.jetbrains.plugin.structure.base.utils

interface Version<V: Version<V>> : Comparable<V> {
  val productCode: String
  fun asString(): String
  fun asStringWithoutProductCode(): String
  fun setProductCodeIfAbsent(productCode: String): V
}