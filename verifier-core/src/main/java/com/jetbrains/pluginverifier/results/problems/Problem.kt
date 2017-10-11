package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.results.presentation.Presentable

/**
 * @author Sergey Patrikeev
 */
abstract class Problem : Presentable {

  abstract val shortDescription: String

  abstract val fullDescription: String

  final override val shortPresentation: String
    get() = shortDescription

  override val fullPresentation: String
    get() = fullDescription

  final override fun toString(): String = fullDescription

  final override fun equals(other: Any?): Boolean = other is Problem && fullDescription == other.fullDescription

  final override fun hashCode(): Int = fullDescription.hashCode()

}