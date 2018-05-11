package com.jetbrains.pluginverifier.results.problems

/**
 * Base class for all the binary compatibility problems.
 *
 * Each problem has a [short] [shortDescription] description
 * which can be used to group similar problems of several plugins,
 * and a [full] [fullDescription] description containing all the details,
 * such as a problem location in the bytecode, the JVM specification
 * reference and a possible problem effect.
 */
abstract class CompatibilityProblem {

  abstract val shortDescription: String

  abstract val fullDescription: String

  abstract override fun equals(other: Any?): Boolean

  abstract override fun hashCode(): Int

  final override fun toString() = fullDescription

}