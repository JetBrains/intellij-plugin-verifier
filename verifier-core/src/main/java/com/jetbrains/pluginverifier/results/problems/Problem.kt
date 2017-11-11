package com.jetbrains.pluginverifier.results.problems

/**
 * @author Sergey Patrikeev
 */
abstract class Problem {

  abstract val shortDescription: String

  abstract val fullDescription: String

  protected open val equalityReference: String
    get() = fullDescription

  final override fun toString(): String = fullDescription

  final override fun equals(other: Any?): Boolean = other is Problem && equalityReference == other.equalityReference

  final override fun hashCode(): Int = equalityReference.hashCode()

}