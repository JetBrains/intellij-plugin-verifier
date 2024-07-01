/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.overrideOnly

import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.usages.FilteringApiUsageProcessor
import com.jetbrains.pluginverifier.usages.util.findEffectiveMemberAnnotation
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.filter.CompositeApiUsageFilter
import com.jetbrains.pluginverifier.verifiers.filter.SameModuleUsageFilter
import com.jetbrains.pluginverifier.verifiers.hasAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.ClassUsageType
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.searchParentOverrides
import org.objectweb.asm.tree.AbstractInsnNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val LOG: Logger = LoggerFactory.getLogger(OverrideOnlyMethodUsageProcessor::class.java)
private const val overrideOnlyAnnotationName = "org/jetbrains/annotations/ApiStatus\$OverrideOnly"

private val overrideOnlyUsageFilter = CompositeApiUsageFilter(
  SameModuleUsageFilter(overrideOnlyAnnotationName),
  CallOfSuperConstructorOverrideOnlyAllowedUsageFilter(),
  DelegateCallOnOverrideOnlyUsageFilter().withBridgeMethodSupport(),
  SuperclassCallOnOverrideOnlyUsageFilter()
)

class OverrideOnlyMethodUsageProcessor(private val overrideOnlyRegistrar: OverrideOnlyRegistrar) :
  FilteringApiUsageProcessor(overrideOnlyUsageFilter) {

  /**
   * Processes the invocation of a method, if allowed.
   *
   * Example:
   * ```
   *   public void usages(OverrideOnlyMethodOwner owner1,) {
   *     owner1.overrideOnlyMethod();
   *   }
   * ```
   * Description:
   * - Both _Invoked Method Reference_ and _Invoked Method_ will refer to the `overrideOnlyMethod`.
   * - _Invocation Instruction_ will refer to the JVM bytecode instruction which will invoke the `overrideOnlyMethod`.
   * In this example, this will be `182/invokevirtual` opcode.
   * - _Caller Method_ refers to the `usages()` method
   * @param invokedMethodReference a reference to the `OverrideOnly` method that is being invoked
   * @param invokedMethod the `OverrideOnly` method that is being invoked
   * @param invocationInstruction low-level JVM invocation instruction used to invoke the `OverrideOnly` method
   * @param callerMethod the method in which the invocation of `OverrideOnly` method occurs.
   * @param context the verification context with additional metadata and data
   *
   */
  override fun doProcessMethodInvocation(
    invokedMethodReference: MethodReference,
    invokedMethod: Method,
    invocationInstruction: AbstractInsnNode,
    callerMethod: Method,
    context: VerificationContext
  ) {

    if (invokedMethod.isOverrideOnlyMethod(context)) {
      overrideOnlyRegistrar.registerOverrideOnlyMethodUsage(
        OverrideOnlyMethodUsage(invokedMethodReference, invokedMethod.location, callerMethod.location)
      )
    }
  }


  private fun Method.isOverrideOnlyMethod(context: VerificationContext): Boolean =
    annotations.hasAnnotation(overrideOnlyAnnotationName)
      || containingClassFile.annotations.hasAnnotation(overrideOnlyAnnotationName)
      || isAnnotationPresent(overrideOnlyAnnotationName, context)

  private fun Method.isAnnotationPresent(annotationFqn: String, verificationContext: VerificationContext): Boolean {
    if (findEffectiveMemberAnnotation(annotationFqn, verificationContext.classResolver) != null) {
      return true
    }

    val overriddenMethod =
      searchParentOverrides(verificationContext.classResolver).firstOrNull { (overriddenMethod, c) ->
        overriddenMethod.findEffectiveMemberAnnotation(annotationFqn, verificationContext.classResolver) != null
      }
    return if (overriddenMethod == null) {
      LOG.atTrace().log("No overridden method for $name is annotated by [$annotationFqn]")
      false
    } else {
      LOG.atDebug()
        .log("Method '${overriddenMethod.method.name}' in [${overriddenMethod.klass.name}] is annotated by [$annotationFqn]")
      true
    }
  }


  override fun doProcessClassReference(
    classReference: ClassReference,
    resolvedClass: ClassFile,
    referrer: ClassFileMember,
    classUsageType: ClassUsageType,
    context: VerificationContext
  ) = Unit

  override fun doProcessFieldAccess(
    fieldReference: FieldReference,
    resolvedField: Field,
    callerMethod: Method,
    context: VerificationContext
  ) = Unit

}

private fun DelegateCallOnOverrideOnlyUsageFilter.withBridgeMethodSupport() =
  BridgeMethodOverrideUsageFilter(this)




