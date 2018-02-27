package com.jetbrains.pluginverifier.parameters.filtering

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.problems.*
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext

/**
 * Ignores compatibility problems that originate from
 * Android source code, which is author by Google team
 * and gets merged into IDEA codebase.
 * Those problems should be tracked in a separate configuration.
 */
class AndroidProblemsFilter : ProblemsFilter {

  private companion object {
    val androidPackages = listOf("com/android", "org/jetbrains/android")

    fun String.belongsToPackage(packageName: String) =
        startsWith(packageName + "/")

    fun ClassLocation.belongsToPackage(packageName: String) =
        className.belongsToPackage(packageName)

    fun ClassReference.belongsToPackage(packageName: String) =
        className.belongsToPackage(packageName)

    fun ClassLocation.belongsToAndroid() = androidPackages.any { belongsToPackage(it) }

    fun ClassReference.belongsToAndroid() = androidPackages.any { belongsToPackage(it) }
  }

  override fun shouldReportProblem(
      problem: CompatibilityProblem,
      verificationContext: VerificationContext
  ): ProblemsFilter.Result {
    val ignore = with(problem) {
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
        is InvalidClassFileProblem -> brokenClass.belongsToAndroid()
        is InvokeClassMethodOnInterfaceProblem -> changedClass.belongsToAndroid()
        is InvokeInterfaceMethodOnClassProblem -> changedInterface.belongsToAndroid()
        is InvokeInterfaceOnPrivateMethodProblem -> resolvedMethod.hostClass.belongsToAndroid()
        is InvokeNonStaticInstructionOnStaticMethodProblem -> resolvedMethod.hostClass.belongsToAndroid()
        is InvokeStaticOnNonStaticMethodProblem -> resolvedMethod.hostClass.belongsToAndroid()
        is MethodNotFoundProblem -> unresolvedMethod.hostClass.belongsToAndroid()
        is MethodNotImplementedProblem -> abstractMethod.hostClass.belongsToAndroid()
        is MultipleDefaultImplementationsProblem -> methodReference.hostClass.belongsToAndroid()
        is NonStaticAccessOfStaticFieldProblem -> field.hostClass.belongsToAndroid()
        is OverridingFinalMethodProblem -> finalMethod.hostClass.belongsToAndroid()
        is StaticAccessOfNonStaticFieldProblem -> field.hostClass.belongsToAndroid()
        is SuperClassBecameInterfaceProblem -> interfaze.belongsToAndroid()
        is SuperInterfaceBecameClassProblem -> clazz.belongsToAndroid()
        else -> false
      }
    }
    return if (ignore) {
      ProblemsFilter.Result.Ignore("the problem belongs to Android code, which is authored by Google team and gets merged into IDEA sources")
    } else {
      ProblemsFilter.Result.Report
    }
  }
}