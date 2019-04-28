package com.jetbrains.pluginverifier.parameters.filtering

import com.jetbrains.pluginverifier.results.problems.*
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext

/**
 * [ProblemsFilter] that yields only Android related problems.
 */
class AndroidProblemsFilter : ProblemsFilter {

  override fun shouldReportProblem(
      problem: CompatibilityProblem,
      context: PluginVerificationContext
  ): ProblemsFilter.Result {
    return if (problem.relatesToAndroid()) {
      ProblemsFilter.Result.Report
    } else {
      ProblemsFilter.Result.Ignore("the problem doesn't belong to Android subsystem")
    }
  }
}

private val androidPackages = listOf("com.android", "org.jetbrains.android").map { it.replace('.', '/') }

private fun CompatibilityProblem.relatesToAndroid(): Boolean {
  val classOrPackage = when (this) {
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
    is PackageNotFoundProblem -> packageName
    else -> return false
  }
  return androidPackages.any { classOrPackage == it || classOrPackage.startsWith("$it/") }
}
