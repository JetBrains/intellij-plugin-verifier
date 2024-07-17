package com.jetbrains.pluginverifier.usages.internal

import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.toReference
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.usages.util.isFromVerifiedPlugin
import com.jetbrains.pluginverifier.verifiers.ProblemRegistrar
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.ClassUsageType
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.MethodResolver
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning
import com.jetbrains.pluginverifier.warnings.WarningRegistrar
import org.objectweb.asm.tree.AbstractInsnNode

abstract class BaseInternalApiUsageProcessor(
  private val internalUsageRegistrar: InternalUsageRegistrar
) :
  ApiUsageProcessor {

  override fun processClassReference(
    classReference: ClassReference,
    resolvedClass: ClassFile,
    context: VerificationContext,
    referrer: ClassFileMember,
    classUsageType: ClassUsageType
  ) {
    val usageLocation = referrer.location
    if (isInternal(resolvedClass, context, usageLocation) && context.isFromVerifiedPlugin(referrer)) {
      internalUsageRegistrar.registerClass(classReference, resolvedClass.location, usageLocation)
    }
  }

  override fun processMethodInvocation(
    methodReference: MethodReference,
    resolvedMethod: Method,
    instructionNode: AbstractInsnNode,
    callerMethod: Method,
    context: VerificationContext
  ) {
    val usageLocation = callerMethod.location
    if (isInternal(resolvedMethod, context, usageLocation)) {
      // Check if the method is an override, and if so check top declaration
      val canBeOverridden = !resolvedMethod.isStatic && !resolvedMethod.isPrivate
        && resolvedMethod.name != "<init>" && resolvedMethod.name != "<clinit>"

      // Taken from MethodOverridingVerifier
      val overriddenMethod = if (canBeOverridden) {
        MethodResolver().resolveMethod(
          ClassFileWithNoMethodsWrapper(resolvedMethod.containingClassFile),
          resolvedMethod.location.toReference(),
          if (resolvedMethod.containingClassFile.isInterface) Instruction.INVOKE_INTERFACE else Instruction.INVOKE_VIRTUAL,
          resolvedMethod,
          VerificationContextWithSilentProblemRegistrar(context)
        )
      } else {
        null
      }

      if (overriddenMethod == null || isInternal(overriddenMethod, context, usageLocation)) {
        internalUsageRegistrar.registerMethod(methodReference, resolvedMethod.location, usageLocation)
      }
    }
  }

  private class ClassFileWithNoMethodsWrapper(
    private val classFile: ClassFile
  ) : ClassFile by classFile {
    override val methods: Sequence<Method> get() = emptySequence()
  }

  private class VerificationContextWithSilentProblemRegistrar(
    private val delegate: VerificationContext
  ) : VerificationContext by delegate {
    override val problemRegistrar: ProblemRegistrar = object : ProblemRegistrar {
      override fun registerProblem(problem: CompatibilityProblem) = Unit
    }

    override val warningRegistrar: WarningRegistrar = object : WarningRegistrar {
      override fun registerCompatibilityWarning(warning: CompatibilityWarning) = Unit
    }
  }

  override fun processFieldAccess(
    fieldReference: FieldReference,
    resolvedField: Field,
    context: VerificationContext,
    callerMethod: Method
  ) {
    val usageLocation = callerMethod.location
    if (isInternal(resolvedField, context, usageLocation)) {
      internalUsageRegistrar.registerField(fieldReference, resolvedField.location, usageLocation)
    }
  }

  protected abstract fun isInternal(
    resolvedMember: ClassFileMember,
    context: VerificationContext,
    usageLocation: Location
  ): Boolean

}