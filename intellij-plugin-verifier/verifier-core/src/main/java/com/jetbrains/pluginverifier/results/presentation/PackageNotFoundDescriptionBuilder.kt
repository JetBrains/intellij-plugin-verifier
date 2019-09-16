package com.jetbrains.pluginverifier.results.presentation

import com.jetbrains.plugin.structure.base.utils.pluralize
import com.jetbrains.plugin.structure.base.utils.pluralizeWithNumber
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.results.problems.PackageNotFoundProblem

/**
 * Utility builder of "package not found" message.
 */
object PackageNotFoundDescriptionBuilder {

  private const val NOT_FOUND_CLASSES_SAMPLES = 5

  private const val NOT_FOUND_LOCATIONS = 5

  private const val MINIMUM_HIDDEN = 3

  /**
   * Build full description of [PackageNotFoundProblem].
   */
  fun buildDescription(packageNotFoundProblem: PackageNotFoundProblem) = buildString {
    val classNotFoundProblems = packageNotFoundProblem.classNotFoundProblems
    val missingClasses = classNotFoundProblems.mapTo(hashSetOf()) { it.unresolved.className }
    val missingClassesNumber = missingClasses.size
    val normalPackageName = packageNotFoundProblem.packageName.replace('/', '.')

    append("Package '$normalPackageName'")
    append(" is not found along with its ")
    if (missingClassesNumber > 1) {
      append("class".pluralizeWithNumber(missingClassesNumber))
    } else {
      append("class " + toFullJavaClassName(missingClasses.first()))
    }
    appendln(".")

    appendln(
        "Probably the package '$normalPackageName' belongs to a library or dependency that is not resolved by the checker.\n" +
            "It is also possible, however, that this package was actually removed from a dependency causing the detected problems. " +
            "Access to unresolved classes at runtime may lead to **NoSuchClassError**."
    )

    val (showClasses, hideClasses) = if (missingClassesNumber < NOT_FOUND_CLASSES_SAMPLES + MINIMUM_HIDDEN) {
      missingClassesNumber to 0
    } else {
      NOT_FOUND_CLASSES_SAMPLES to (missingClassesNumber - NOT_FOUND_CLASSES_SAMPLES)
    }

    append("The following classes of '$normalPackageName' are not resolved")
    if (hideClasses > 0) {
      appendln(" (only $showClasses most used classes are shown, $hideClasses hidden):")
    } else {
      appendln(":")
    }

    /**
     * Group unresolved classes and sort by number of occurrences.
     */
    val classRefToProblems = classNotFoundProblems
        .groupBy { it.unresolved }
        .toList()
        .sortedWith(Comparator { one, two ->
          if (one.second.size != two.second.size) {
            two.second.size.compareTo(one.second.size)
          } else {
            one.first.className.compareTo(two.first.className)
          }
        })

    for ((classRef, problems) in classRefToProblems.take(showClasses)) {
      val (showLocations, hideLocations) = if (problems.size < NOT_FOUND_LOCATIONS + MINIMUM_HIDDEN) {
        problems.size to 0
      } else {
        NOT_FOUND_LOCATIONS to (problems.size - NOT_FOUND_LOCATIONS)
      }

      val differentProblems = selectFromDifferentLocations(showLocations, problems)
      appendln("  Class " + classRef.formatClassReference(ClassOption.FULL_NAME) + " is referenced in")
      differentProblems.map { it.usage }.sortedWith(locationsComparator).forEach {
        appendln("    $it")
      }
      if (hideLocations > 0) {
        appendln("    ...and $hideLocations other " + "place".pluralize(hideLocations) + "...")
      }
    }
  }

  /**
   * Select up to [number] problems of type [ClassNotFoundProblem]
   * from as many different locations as possible.
   */
  private fun selectFromDifferentLocations(
      number: Int,
      problems: List<ClassNotFoundProblem>
  ): List<ClassNotFoundProblem> {
    if (problems.size <= number) {
      return problems
    }

    val hostClassToProblems = problems
        .groupBy { it.usage.hostClass() }
        .mapValues { it.value.sortedBy { it.usage.presentableLocation } }

    val result = arrayListOf<ClassNotFoundProblem>()
    var index = 0
    while (result.size < number) {
      //Problems from different locations at this index.
      val slice = hostClassToProblems.mapNotNull { it.value.elementAtOrNull(index) }
      result.addAll(slice)
      index++
    }
    if (result.size > number) {
      return result.take(number)
    }
    return result
  }

  private val locationsComparator = Comparator<Location> r@{ one, two ->
    if (one is ClassLocation && two is ClassLocation) {
      return@r one.className.compareTo(two.className)
    }
    if (one is MethodLocation && two is MethodLocation) {
      return@r compareBy<MethodLocation> { it.hostClass.className }
          .thenBy { it.methodName }
          .compare(one, two)
    }
    if (one is FieldLocation && two is FieldLocation) {
      return@r compareBy<FieldLocation> { it.hostClass.className }
          .thenBy { it.fieldName }
          .compare(one, two)
    }
    return@r one.presentableLocation.compareTo(two.presentableLocation)
  }

  private fun Location.hostClass() = when (this) {
    is ClassLocation -> this
    is MethodLocation -> hostClass
    is FieldLocation -> hostClass
  }

}