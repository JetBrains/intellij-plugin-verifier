package com.jetbrains.pluginverifier.results.problems

/**
 * Base class for all compatibility problems.
 *
 * Each problem has a [short] [shortDescription] description
 * which can be used to group similar problems of several plugins,
 * and a [full] [fullDescription] description containing all the details,
 * such as problem's location in bytecode, JVM specification
 * reference and possible effect.
 *
 * Each problem has a certain [problemType],
 * which can be used to group similar problems.
 */
abstract class CompatibilityProblem {

  abstract val problemType: String

  abstract val shortDescription: String

  abstract val fullDescription: String

  abstract override fun equals(other: Any?): Boolean

  abstract override fun hashCode(): Int

  final override fun toString() = fullDescription

}