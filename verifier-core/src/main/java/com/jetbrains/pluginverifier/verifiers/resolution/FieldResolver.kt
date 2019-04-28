package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.results.access.AccessType
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedFieldUsage
import com.jetbrains.pluginverifier.results.experimental.ExperimentalFieldUsage
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.problems.FieldNotFoundProblem
import com.jetbrains.pluginverifier.results.problems.IllegalFieldAccessProblem
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.getDeprecationInfo
import com.jetbrains.pluginverifier.verifiers.hierarchy.ClassHierarchyBuilder
import com.jetbrains.pluginverifier.verifiers.isExperimentalApi
import com.jetbrains.pluginverifier.verifiers.isSubclassOf

/**
 * Utility class that implements fields resolution strategy,
 * as described in the JVM specification (§5.4.3.2).
 */
class FieldResolver {

  fun resolveField(
      classFile: ClassFile,
      fieldReference: FieldReference,
      context: VerificationContext,
      callerMethod: Method,
      instruction: Instruction
  ): Field? =
      when (val resolutionResult = doResolveField(classFile, fieldReference, context)) {
        FieldResolutionResult.Abort -> null
        FieldResolutionResult.NotFound -> {
          registerFieldNotFoundProblem(context, fieldReference, instruction, callerMethod, classFile)
          null
        }
        is FieldResolutionResult.Found -> {
          checkFieldIsAccessible(resolutionResult.field, fieldReference, callerMethod, instruction, context)
          checkFieldIsUnstable(resolutionResult.field, callerMethod, context)
          resolutionResult.field
        }
      }

  private sealed class FieldResolutionResult {
    object Abort : FieldResolutionResult()

    object NotFound : FieldResolutionResult()

    data class Found(val field: Field) : FieldResolutionResult()
  }

  private fun doResolveField(
      classFile: ClassFile,
      fieldReference: FieldReference,
      context: VerificationContext
  ): FieldResolutionResult {
    /**
     * 1) Firstly, the field is searched in the class of the field reference.
     */
    val matchingField = classFile.fields.firstOrNull { it.name == fieldReference.fieldName && it.descriptor == fieldReference.fieldDescriptor }
    if (matchingField != null) {
      return FieldResolutionResult.Found(matchingField)
    }

    /**
     * 2) Otherwise, field lookup is applied recursively to the direct superinterfaces of the specified class or interface C.
     */
    for (anInterface in classFile.interfaces) {
      val resolvedInterface = context.classResolver.resolveClassChecked(anInterface, classFile, context)
          ?: return FieldResolutionResult.Abort

      when (val lookupResult = doResolveField(resolvedInterface, fieldReference, context)) {
        FieldResolutionResult.NotFound -> Unit
        FieldResolutionResult.Abort -> return lookupResult
        is FieldResolutionResult.Found -> return lookupResult
      }
    }

    /**
     * 3) Otherwise, if C has a superclass S, field lookup is applied recursively to S.
     */
    val superName = classFile.superName
    if (superName != null) {
      val resolvedSuper = context.classResolver.resolveClassChecked(superName, classFile, context)
          ?: return FieldResolutionResult.Abort

      when (val lookupResult = doResolveField(resolvedSuper, fieldReference, context)) {
        FieldResolutionResult.NotFound -> Unit
        FieldResolutionResult.Abort -> return lookupResult
        is FieldResolutionResult.Found -> return lookupResult
      }
    }

    /**
     * 4) Otherwise, field lookup fails.
     */
    return FieldResolutionResult.NotFound
  }

  private fun registerFieldNotFoundProblem(
      context: VerificationContext,
      fieldReference: FieldReference,
      instruction: Instruction,
      callerMethod: Method,
      classFile: ClassFile
  ) {
    val classHierarchy = ClassHierarchyBuilder(context).buildClassHierarchy(classFile)
    context.problemRegistrar.registerProblem(
        FieldNotFoundProblem(fieldReference, callerMethod.location, classHierarchy, instruction)
    )
  }

  private fun checkFieldIsAccessible(
      field: Field,
      fieldReference: FieldReference,
      callerMethod: Method,
      instruction: Instruction,
      context: VerificationContext
  ) {
    var accessProblem: AccessType? = null

    when {
      field.isPrivate -> if (callerMethod.owner.name != field.owner.name) {
        accessProblem = AccessType.PRIVATE
      }
      field.isProtected -> if (callerMethod.owner.packageName != field.owner.packageName) {
        if (!context.classResolver.isSubclassOf(callerMethod.owner, field.owner.name)) {
          accessProblem = AccessType.PROTECTED
        }
      }
      field.isDefaultAccess -> if (callerMethod.owner.packageName != field.owner.packageName) {
        accessProblem = AccessType.PACKAGE_PRIVATE
      }
    }

    if (accessProblem != null) {
      context.problemRegistrar.registerProblem(
          IllegalFieldAccessProblem(
              fieldReference,
              field.location,
              callerMethod.location,
              instruction,
              accessProblem
          )
      )
    }
  }

  private fun checkFieldIsUnstable(field: Field, callerMethod: Method, context: VerificationContext) {
    val fieldDeprecated = field.getDeprecationInfo()
    if (fieldDeprecated != null) {
      context.deprecatedApiRegistrar.registerDeprecatedUsage(DeprecatedFieldUsage(field.location, callerMethod.location, fieldDeprecated))
    }
    if (field.isExperimentalApi()) {
      context.experimentalApiRegistrar.registerExperimentalApiUsage(ExperimentalFieldUsage(field.location, callerMethod.location))
    }
  }

}