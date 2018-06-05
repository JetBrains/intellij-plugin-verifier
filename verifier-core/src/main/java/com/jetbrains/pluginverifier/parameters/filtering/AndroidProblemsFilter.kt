package com.jetbrains.pluginverifier.parameters.filtering

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.problems.*
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext

/**
 * [ProblemsFilter] that yields only Android related problems.
 */
class AndroidProblemsFilter : ProblemsFilter {

  private companion object {
    val androidPackages = listOf("com/android", "org/jetbrains/android")

    fun String.belongsToPackage(packageName: String) =
        startsWith(packageName + "/")

    fun ClassLocation.belongsToAndroid() = androidPackages.any { className.belongsToPackage(it) }

    fun ClassReference.belongsToAndroid() = androidPackages.any { className.belongsToPackage(it) }
  }

  override fun shouldReportProblem(problem: CompatibilityProblem, verificationContext: VerificationContext): ProblemsFilter.Result {
    val report = with(problem) {
      when (this) {
        is AbstractClassInstantiationProblem -> abstractClass.belongsToAndroid()
        is AbstractMethodInvocationProblem -> method.hostClass.belongsToAndroid()
        is ChangeFinalFieldProblem -> field.hostClass.belongsToAndroid()
        is ClassNotFoundProblem -> unresolved.belongsToAndroid()
        is FieldNotFoundProblem -> unresolvedField.hostClass.belongsToAndroid()
        is IllegalClassAccessProblem -> unavailableClass.belongsToAndroid()
        is IllegalFieldAccessProblem -> inaccessibleField.hostClass.belongsToAndroid()
        is IllegalMethodAccessProblem -> inaccessibleMethod.hostClass.belongsToAndroid()
        is InheritFromFinalClassProblem -> finalClass.belongsToAndroid()
        is InterfaceInstantiationProblem -> interfaze.belongsToAndroid()
        is InvalidClassFileProblem -> invalidClass.belongsToAndroid()
        is FailedToReadClassFileProblem -> failedClass.belongsToAndroid()
        is InvokeClassMethodOnInterfaceProblem -> changedClass.belongsToAndroid()
        is InvokeInterfaceMethodOnClassProblem -> changedInterface.belongsToAndroid()
        is InvokeInterfaceOnPrivateMethodProblem -> resolvedMethod.hostClass.belongsToAndroid()
        is InvokeInstanceInstructionOnStaticMethodProblem -> resolvedMethod.hostClass.belongsToAndroid()
        is InvokeStaticOnInstanceMethodProblem -> resolvedMethod.hostClass.belongsToAndroid()
        is MethodNotFoundProblem -> unresolvedMethod.hostClass.belongsToAndroid()
        is MethodNotImplementedProblem -> abstractMethod.hostClass.belongsToAndroid()
        is MultipleDefaultImplementationsProblem -> methodReference.hostClass.belongsToAndroid()
        is InstanceAccessOfStaticFieldProblem -> field.hostClass.belongsToAndroid()
        is OverridingFinalMethodProblem -> finalMethod.hostClass.belongsToAndroid()
        is StaticAccessOfInstanceFieldProblem -> field.hostClass.belongsToAndroid()
        is SuperClassBecameInterfaceProblem -> interfaze.belongsToAndroid()
        is SuperInterfaceBecameClassProblem -> clazz.belongsToAndroid()
        else -> false
      }
    }

    return if (report) {
      ProblemsFilter.Result.Report
    } else {
      ProblemsFilter.Result.Ignore("the problem doesn't belong to Android subsystem")
    }
  }
}