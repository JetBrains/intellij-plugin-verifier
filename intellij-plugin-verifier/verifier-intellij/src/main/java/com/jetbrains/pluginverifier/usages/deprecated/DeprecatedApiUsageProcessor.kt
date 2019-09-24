package com.jetbrains.pluginverifier.usages.deprecated

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.tree.AbstractInsnNode

class DeprecatedApiUsageProcessor(private val deprecatedApiRegistrar: DeprecatedApiRegistrar) : ApiUsageProcessor {
  override fun processClassReference(
      classReference: ClassReference,
      resolvedClass: ClassFile,
      usageLocation: Location,
      context: VerificationContext
  ) {
    val deprecationInfo = resolvedClass.deprecationInfo ?: return
    deprecatedApiRegistrar.registerDeprecatedUsage(
        DeprecatedClassUsage(classReference, resolvedClass.location, usageLocation, deprecationInfo)
    )
  }

  override fun processMethodInvocation(
    methodReference: MethodReference,
    resolvedMethod: Method,
    usageLocation: Location,
    context: VerificationContext,
    instructionNode: AbstractInsnNode
  ) {
    val deprecationInfo = resolvedMethod.deprecationInfo ?: return
    deprecatedApiRegistrar.registerDeprecatedUsage(
        DeprecatedMethodUsage(methodReference, resolvedMethod.location, usageLocation, deprecationInfo)
    )
  }

  override fun processFieldAccess(
      fieldReference: FieldReference,
      resolvedField: Field,
      usageLocation: Location,
      context: VerificationContext
  ) {
    val deprecationInfo = resolvedField.deprecationInfo ?: return
    deprecatedApiRegistrar.registerDeprecatedUsage(
        DeprecatedFieldUsage(fieldReference, resolvedField.location, usageLocation, deprecationInfo)
    )
  }
}