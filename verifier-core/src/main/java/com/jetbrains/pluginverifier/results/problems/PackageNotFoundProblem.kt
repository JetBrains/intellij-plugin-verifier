package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.results.presentation.PackageNotFoundDescriptionBuilder
import java.util.*

/**
 * Groups many [ClassNotFoundProblem] for one package
 * and displays a shortened aggregated message instead
 * of many independent ones.
 */
class PackageNotFoundProblem(
    val packageName: String,
    val classNotFoundProblems: List<ClassNotFoundProblem>
) : CompatibilityProblem() {

  override val problemType
    get() = "Package not found"

  override val shortDescription
    get() = "Package '${packageName.replace('/', '.')}' is not found"

  override val fullDescription: String
    get() = PackageNotFoundDescriptionBuilder.buildDescription(this)

  override fun equals(other: Any?) = other is PackageNotFoundProblem
      && packageName == other.packageName
      && classNotFoundProblems == other.classNotFoundProblems

  override fun hashCode() = Objects.hash(packageName, classNotFoundProblems)
}
