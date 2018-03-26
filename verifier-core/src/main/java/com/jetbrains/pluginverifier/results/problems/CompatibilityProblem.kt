package com.jetbrains.pluginverifier.results.problems

/**
 * Base class for all the binary compatibility problems.
 *
 * Each problem has a [short] [shortDescription] description
 * which can be used to group similar problems of several plugins,
 * and a [full] [fullDescription] description containing all the details,
 * such as a problem location in the bytecode, the JVM specification
 * reference and a possible problem effect.
 *
 * Two [CompatibilityProblem]s are considered equivalent
 * iff they have the same [fullDescription]s.
 */
abstract class CompatibilityProblem {

  abstract val shortDescription: String

  abstract val fullDescription: String

  protected open val equalityReference: String
    get() = fullDescription

  final override fun toString() = fullDescription

  final override fun equals(other: Any?) = other is CompatibilityProblem && equalityReference == other.equalityReference

  final override fun hashCode() = equalityReference.hashCode()

}