package com.jetbrains.pluginverifier.results.problems

/**
 * @author Sergey Patrikeev
 */
abstract class Problem {

  abstract val shortDescription: String

  abstract val fullDescription: String

  final override fun toString(): String = fullDescription

  final override fun equals(other: Any?): Boolean = other is Problem && fullDescription == other.fullDescription

  final override fun hashCode(): Int = fullDescription.hashCode()

}