package com.jetbrains.pluginverifier.results.problems

abstract class CompatibilityProblem {

  abstract val shortDescription: String

  abstract val fullDescription: String

  protected open val equalityReference: String
    get() = fullDescription

  final override fun toString(): String = fullDescription

  final override fun equals(other: Any?): Boolean = other is CompatibilityProblem && equalityReference == other.equalityReference

  final override fun hashCode(): Int = equalityReference.hashCode()

}