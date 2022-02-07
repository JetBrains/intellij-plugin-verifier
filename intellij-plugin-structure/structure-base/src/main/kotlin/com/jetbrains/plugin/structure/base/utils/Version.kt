package com.jetbrains.plugin.structure.base.utils

interface Version<V: Version<V>> : Comparable<V> {
  fun asString(): String
  fun asStringWithoutProductCode(): String
}