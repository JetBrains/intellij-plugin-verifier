package com.jetbrains.pluginverifier.misc

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap

fun <T> List<T>.splitList(condition: (T) -> Boolean): Pair<List<T>, List<T>> {
  val match = arrayListOf<T>()
  val nonMatch = arrayListOf<T>()
  for (t in this) {
    if (condition(t)) {
      match.add(t)
    } else {
      nonMatch.add(t)
    }
  }
  return match to nonMatch
}

fun <T> List<T>.listEndsWith(vararg ending: T): Boolean {
  if (ending.isEmpty()) {
    return true
  }
  if (size < ending.size) {
    return false
  }
  if (size == ending.size) {
    return this == ending.toList()
  }
  return ending.indices.all { index -> ending[index] == this[size - ending.size + index] }
}

/**
 * Creates a Guava multimap using the input map.
 */
fun <K, V> Map<K, Iterable<V>>.multimapFromMap(): Multimap<K, V> {
  val result = ArrayListMultimap.create<K, V>()
  for ((key, values) in this) {
    result.putAll(key, values)
  }
  return result
}

fun <T> T?.singletonOrEmpty(): List<T> = if (this == null) emptyList() else listOf(this)

inline fun <T> buildList(builderAction: MutableList<T>.() -> Unit): List<T> = arrayListOf<T>().apply(builderAction)
