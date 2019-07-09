package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.results.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.results.problems.PackageNotFoundProblem

/**
 * Returns the top-most package of the given [className] that is not available in this [Resolver].
 *
 * If all packages of the specified class exist, `null` is returned.
 * If the class has default (empty) package, and that default package
 * is not available, then "" is returned.
 */
private fun Resolver.getTopMostMissingPackage(className: String): String? {
  if ('/' !in className) {
    return if (containsPackage("")) {
      null
    } else {
      ""
    }
  }
  val packageParts = className.substringBeforeLast('/', "").split('/')
  var superPackage = ""
  for (packagePart in packageParts) {
    if (superPackage.isNotEmpty()) {
      superPackage += '/'
    }
    superPackage += packagePart
    if (!containsPackage(superPackage)) {
      return superPackage
    }
  }
  return null
}

/**
 * Post-processes the verification result and groups many [ClassNotFoundProblem]s into [PackageNotFoundProblem]s,
 * to make the report easier to understand.
 */
fun groupMissingClassesToMissingPackages(context: VerificationContext) {
  val classNotFoundProblems = context.problemRegistrar.allProblems.filterIsInstance<ClassNotFoundProblem>()

  /**
   * All [ClassNotFoundProblem]s will be split into 2 parts:
   * 1) Independent [ClassNotFoundProblem]s for classes
   * that originate from found packages.
   * These classes seem to be removed, causing API breakages.
   *
   * 2) Grouped [PackageNotFoundProblem]s for several [ClassNotFoundProblem]s
   * for packages that are not found.
   * These missing packages might have been removed,
   * or the Verifier is not properly configured to find them.
   */
  val noClassProblems = hashSetOf<ClassNotFoundProblem>()
  val packageToMissingProblems = hashMapOf<String, MutableSet<ClassNotFoundProblem>>()

  for (classNotFoundProblem in classNotFoundProblems) {
    val className = classNotFoundProblem.unresolved.className
    val missingPackage = context.classResolver.getTopMostMissingPackage(className)
    if (missingPackage != null) {
      packageToMissingProblems
          .getOrPut(missingPackage) { hashSetOf() }
          .add(classNotFoundProblem)
    } else {
      noClassProblems.add(classNotFoundProblem)
    }
  }

  val packageNotFoundProblems = packageToMissingProblems.map { (packageName, missingClasses) ->
    PackageNotFoundProblem(packageName, missingClasses)
  }

  //Retain all individual [ClassNotFoundProblem]s.
  for (problem in classNotFoundProblems) {
    if (problem !in noClassProblems) {
      context.problemRegistrar.unregisterProblem(problem)
    }
  }

  for (packageNotFoundProblem in packageNotFoundProblems) {
    context.problemRegistrar.registerProblem(packageNotFoundProblem)
  }

}