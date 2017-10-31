package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.core.VerificationResultHolder
import com.jetbrains.pluginverifier.misc.impossible
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.problems.Problem

data class VerificationContext(
    val classLoader: Resolver,
    val ideClassLoader: Resolver,
    val resultHolder: VerificationResultHolder,
    val externalClassesPrefixes: List<String>,
    val findDeprecatedApiUsages: Boolean
) {

  fun registerProblem(problem: Problem) {
    resultHolder.registerProblem(problem)
  }

  fun registerDeprecatedUsage(deprecatedApiUsage: DeprecatedApiUsage) {
    if (findDeprecatedApiUsages) {
      val deprecatedElement = deprecatedApiUsage.deprecatedElement
      val hostClass = when (deprecatedElement) {
        is ClassLocation -> deprecatedElement
        is MethodLocation -> deprecatedElement.hostClass
        is FieldLocation -> deprecatedElement.hostClass
        else -> impossible()
      }
      if (isIdeClass(hostClass.className)) {
        resultHolder.registerDeprecatedUsage(deprecatedApiUsage)
      }
    }
  }

  private fun isIdeClass(className: String): Boolean = ideClassLoader.containsClass(className)

  fun isExternalClass(className: String): Boolean = externalClassesPrefixes.any { it.isNotEmpty() && className.startsWith(it) }

}