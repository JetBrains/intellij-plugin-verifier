package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.results.deprecated.DeprecatedClassUsage
import com.jetbrains.pluginverifier.results.deprecated.DiscouragingJdkClassUsage
import com.jetbrains.pluginverifier.results.experimental.ExperimentalClassUsage
import com.jetbrains.pluginverifier.results.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.results.problems.FailedToReadClassFileProblem
import com.jetbrains.pluginverifier.results.problems.IllegalClassAccessProblem
import com.jetbrains.pluginverifier.results.problems.InvalidClassFileProblem
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.getDeprecationInfo
import com.jetbrains.pluginverifier.verifiers.isExperimentalApi

abstract class IntelliJClassResolver : ClassResolver {
  protected abstract fun doResolveClass(className: String): ClassResolutionResult

  final override fun resolveClassOrNull(className: String): ClassFile? {
    val resolutionResult = doResolveClass(className)
    return (resolutionResult as? ClassResolutionResult.Found)?.classFile
  }

  final override fun resolveClassChecked(
      className: String,
      referrer: ClassFileMember,
      context: VerificationContext
  ): ClassFile? =
      when (val resolutionResult = doResolveClass(className)) {
        ClassResolutionResult.NotFound -> {
          context.problemRegistrar.registerProblem(
              ClassNotFoundProblem(ClassReference(className), referrer.location)
          )
          null
        }
        ClassResolutionResult.ExternalClass -> null
        is ClassResolutionResult.InvalidClassFile -> {
          context.problemRegistrar.registerProblem(
              InvalidClassFileProblem(ClassReference(className), referrer.location, resolutionResult.message)
          )
          null
        }
        is ClassResolutionResult.FailedToReadClassFile -> {
          context.problemRegistrar.registerProblem(
              FailedToReadClassFileProblem(ClassReference(className), referrer.location, resolutionResult.reason)
          )
          null
        }
        is ClassResolutionResult.Found -> {
          val classFile = resolutionResult.classFile
          if (!isClassAccessibleToOtherClass(classFile, referrer.containingClassFile)) {
            context.problemRegistrar.registerProblem(
                IllegalClassAccessProblem(classFile.location, classFile.accessType, referrer.location)
            )
          }
          val classDeprecated = classFile.getDeprecationInfo()
          if (classDeprecated != null) {
            context.deprecatedApiRegistrar.registerDeprecatedUsage(
                DeprecatedClassUsage(classFile.location, referrer.location, classDeprecated)
            )
          }
          if (classFile.isDiscouragingJdkClass()) {
            val classOrigin = classFile.classFileOrigin
            if (classOrigin is IntelliJClassFileOrigin.IdeClass || classOrigin is IntelliJClassFileOrigin.JdkClass) {
              val isClassProvidedByIde = classOrigin is IntelliJClassFileOrigin.IdeClass
              context.deprecatedApiRegistrar.registerDeprecatedUsage(
                  DiscouragingJdkClassUsage(classFile.location, referrer.location, isClassProvidedByIde)
              )
            }
          }
          val experimentalApi = classFile.isExperimentalApi()
          if (experimentalApi) {
            context.experimentalApiRegistrar.registerExperimentalApiUsage(
                ExperimentalClassUsage(classFile.location, referrer.location)
            )
          }
          classFile
        }
      }

  sealed class ClassResolutionResult {

    object NotFound : ClassResolutionResult()

    object ExternalClass : ClassResolutionResult()

    data class InvalidClassFile(val message: String) : ClassResolutionResult()

    data class FailedToReadClassFile(val reason: String) : ClassResolutionResult()

    data class Found(val classFile: ClassFile) : ClassResolutionResult()
  }
}