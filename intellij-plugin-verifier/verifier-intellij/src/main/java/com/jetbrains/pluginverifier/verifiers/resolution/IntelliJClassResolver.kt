package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.results.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.results.problems.FailedToReadClassFileProblem
import com.jetbrains.pluginverifier.results.problems.IllegalClassAccessProblem
import com.jetbrains.pluginverifier.results.problems.InvalidClassFileProblem
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext

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
          val usageLocation = referrer.location
          context.apiUsageProcessors.forEach { it.processApiUsage(classFile, usageLocation, context) }
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