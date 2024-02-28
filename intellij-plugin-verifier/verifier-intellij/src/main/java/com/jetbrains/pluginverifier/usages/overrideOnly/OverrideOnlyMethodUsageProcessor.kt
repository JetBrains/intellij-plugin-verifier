/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.overrideOnly

import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.usages.util.findEffectiveMemberAnnotation
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.findAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.searchParentOverrides
import org.objectweb.asm.tree.AbstractInsnNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val LOG: Logger = LoggerFactory.getLogger(OverrideOnlyMethodUsageProcessor::class.java)

class OverrideOnlyMethodUsageProcessor(private val overrideOnlyRegistrar: OverrideOnlyRegistrar) : ApiUsageProcessor {

  private val anActionUpdateMethodAllowedFilter = AnActionUpdateMethodAllowedUsageFilter()

  override fun processMethodInvocation(
    methodReference: MethodReference,
    resolvedMethod: Method,
    instructionNode: AbstractInsnNode,
    callerMethod: Method,
    context: VerificationContext
  ) {


    if (resolvedMethod.isOverrideOnlyMethod(context)
      && !isAllowedOverrideOnlyUsage(methodReference, resolvedMethod, instructionNode, callerMethod, context))
    {
      overrideOnlyRegistrar.registerOverrideOnlyMethodUsage(
        OverrideOnlyMethodUsage(methodReference, resolvedMethod.location, callerMethod.location)
      )
    }
  }

  private fun isAllowedOverrideOnlyUsage(methodReference: MethodReference,
                                         resolvedMethod: Method,
                                         instructionNode: AbstractInsnNode,
                                         callerMethod: Method,
                                         context: VerificationContext): Boolean {
    return isCallOfSuperConstructor(callerMethod, resolvedMethod)
      || anActionUpdateMethodAllowedFilter.allowMethodInvocation(methodReference, resolvedMethod, instructionNode, callerMethod, context)
  }

  private fun isCallOfSuperConstructor(callerMethod: Method, resolvedMethod: Method) =
    resolvedMethod.isConstructor
      && callerMethod.isConstructor
      && callerMethod.containingClassFile.superName == resolvedMethod.containingClassFile.name

  private fun Method.isOverrideOnlyMethod(context: VerificationContext): Boolean =
    annotations.findAnnotation(overrideOnlyAnnotationName) != null
      || containingClassFile.annotations.findAnnotation(overrideOnlyAnnotationName) != null
      || isAnnotationPresent(overrideOnlyAnnotationName, context)

  private fun Method.isAnnotationPresent(annotationFqn: String, verificationContext: VerificationContext): Boolean {
    if (findEffectiveMemberAnnotation(annotationFqn, verificationContext.classResolver) != null) {
      return true
    }

    val overriddenMethod = searchParentOverrides(verificationContext.classResolver).firstOrNull { (overriddenMethod, c) ->
       overriddenMethod.findEffectiveMemberAnnotation(annotationFqn, verificationContext.classResolver) != null
    }
    return if (overriddenMethod == null) {
      LOG.trace("No overridden method for $name is annotated by [$annotationFqn]")
      false
    } else {
      LOG.debug("Method '${overriddenMethod.method.name}' in [${overriddenMethod.klass.name}] is annotated by [$annotationFqn]")
      true
    }
  }


  private companion object {
    const val overrideOnlyAnnotationName = "org/jetbrains/annotations/ApiStatus\$OverrideOnly"
  }







}


