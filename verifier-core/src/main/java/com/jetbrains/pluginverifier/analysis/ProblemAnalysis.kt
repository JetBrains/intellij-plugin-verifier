package com.jetbrains.pluginverifier.analysis

import com.jetbrains.pluginverifier.ResultHolder
import com.jetbrains.pluginverifier.results.problems.*
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClsResolver

/**
 * Returns name of the class that induces the problem.
 *
 * It can be used to determine if this [CompatibilityProblem]
 * must be reported based on the class's package.
 */
fun CompatibilityProblem.getClassInducingProblem(): String? {
  return when (this) {
    is AbstractClassInstantiationProblem -> abstractClass.className
    is AbstractMethodInvocationProblem -> method.hostClass.className
    is ChangeFinalFieldProblem -> field.hostClass.className
    is ClassNotFoundProblem -> unresolved.className
    is FieldNotFoundProblem -> unresolvedField.hostClass.className
    is IllegalClassAccessProblem -> unavailableClass.className
    is IllegalFieldAccessProblem -> inaccessibleField.hostClass.className
    is IllegalMethodAccessProblem -> inaccessibleMethod.hostClass.className
    is InheritFromFinalClassProblem -> finalClass.className
    is InterfaceInstantiationProblem -> interfaze.className
    is InvalidClassFileProblem -> invalidClass.className
    is FailedToReadClassFileProblem -> failedClass.className
    is InvokeClassMethodOnInterfaceProblem -> changedClass.className
    is InvokeInterfaceMethodOnClassProblem -> changedInterface.className
    is InvokeInterfaceOnPrivateMethodProblem -> resolvedMethod.hostClass.className
    is InvokeInstanceInstructionOnStaticMethodProblem -> resolvedMethod.hostClass.className
    is InvokeStaticOnInstanceMethodProblem -> resolvedMethod.hostClass.className
    is MethodNotFoundProblem -> unresolvedMethod.hostClass.className
    is MethodNotImplementedProblem -> abstractMethod.hostClass.className
    is MultipleDefaultImplementationsProblem -> methodReference.hostClass.className
    is InstanceAccessOfStaticFieldProblem -> field.hostClass.className
    is OverridingFinalMethodProblem -> finalMethod.hostClass.className
    is StaticAccessOfInstanceFieldProblem -> field.hostClass.className
    is SuperClassBecameInterfaceProblem -> interfaze.className
    is SuperInterfaceBecameClassProblem -> clazz.className
    else -> null
  }
}

/**
 * Returns the top-most package of the given [className] that is not available in this [ClsResolver].
 *
 * If all packages of the specified class exist, `null` is returned.
 * If the class has default (empty) package, and that default package
 * is not available, then "" is returned.
 */
private fun ClsResolver.getTopMostMissingPackage(className: String): String? {
  if ('/' !in className) {
    return if (packageExists("")) {
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
    if (!packageExists(superPackage)) {
      return superPackage
    }
  }
  return null
}

/**
 * Post-processes the verification result and groups
 * many [ClassNotFoundProblem]s into [PackageNotFoundProblem]s,
 * to make the report easier to understand.
 */
fun VerificationContext.analyzeMissingClasses(resultHolder: ResultHolder) {
  val classNotFoundProblems = resultHolder.compatibilityProblems.filterIsInstance<ClassNotFoundProblem>()

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
  val packageToMissingProblems = hashMapOf<String, MutableList<ClassNotFoundProblem>>()

  for (classNotFoundProblem in classNotFoundProblems) {
    val className = classNotFoundProblem.unresolved.className
    val missingPackage = clsResolver.getTopMostMissingPackage(className)
    if (missingPackage != null) {
      packageToMissingProblems
          .getOrPut(missingPackage) { arrayListOf() }
          .add(classNotFoundProblem)
    } else {
      noClassProblems.add(classNotFoundProblem)
    }
  }

  val packageNotFoundProblems = packageToMissingProblems.map { (packageName, missingClasses) ->
    PackageNotFoundProblem(packageName, missingClasses)
  }

  //Retain all individual [ClassNotFoundProblem]s.
  resultHolder.compatibilityProblems.retainAll {
    it !is ClassNotFoundProblem || it in noClassProblems
  }

  //Add grouped [PackageNotFoundProblem]s via [registerProblem]
  //to ignore the problems if needed.
  for (packageNotFoundProblem in packageNotFoundProblems) {
    registerProblem(packageNotFoundProblem)
  }

}