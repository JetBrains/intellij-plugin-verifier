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
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.VarInsnNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val LOG: Logger = LoggerFactory.getLogger(OverrideOnlyMethodUsageProcessor::class.java)

class OverrideOnlyMethodUsageProcessor(private val overrideOnlyRegistrar: OverrideOnlyRegistrar) : ApiUsageProcessor {

  override fun processMethodInvocation(
    methodReference: MethodReference,
    resolvedMethod: Method,
    instructionNode: AbstractInsnNode,
    callerMethod: Method,
    context: VerificationContext
  ) {
    if (resolvedMethod.isOverrideOnlyMethod(context) && !isAllowedOverrideOnlyUsage(callerMethod, resolvedMethod, instructionNode)) {
      overrideOnlyRegistrar.registerOverrideOnlyMethodUsage(
        OverrideOnlyMethodUsage(methodReference, resolvedMethod.location, callerMethod.location)
      )
    }
  }

  private fun isAllowedOverrideOnlyUsage(callerMethod: Method, resolvedMethod: Method, instructionNode: AbstractInsnNode): Boolean {
    return isCallOfSuperConstructor(callerMethod, resolvedMethod)
      || isAnActionUpdateSuperCall(callerMethod, resolvedMethod, instructionNode)
      || isAnActionUpdateDelegateCall(callerMethod, resolvedMethod, instructionNode)
  }

  private fun isCallOfSuperConstructor(callerMethod: Method, resolvedMethod: Method) =
    resolvedMethod.isConstructor
      && callerMethod.isConstructor
      && callerMethod.containingClassFile.superName == resolvedMethod.containingClassFile.name

  private fun isCallOfSuperMethod(callerMethod: Method, resolvedMethod: Method, instructionNode: AbstractInsnNode): Boolean {
    return callerMethod.name == resolvedMethod.name
      && callerMethod.descriptor == resolvedMethod.descriptor
      && instructionNode.opcode == Opcodes.INVOKESPECIAL
  }

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

  private fun isAnActionUpdateSuperCall(callerMethod: Method, resolvedMethod: Method, instructionNode: AbstractInsnNode): Boolean {
    return (resolvedMethod.name == "update"
      && resolvedMethod.descriptor == "(Lcom/intellij/openapi/actionSystem/AnActionEvent;)V"
      && isCallOfSuperMethod(callerMethod, resolvedMethod, instructionNode))
  }

  private fun isAnActionUpdateDelegateCall(callerMethod: Method, resolvedMethod: Method, instruction: AbstractInsnNode): Boolean {
    val isCallingAnActionUpdateMethod =  resolvedMethod.name == "update"
      && resolvedMethod.descriptor == "(Lcom/intellij/openapi/actionSystem/AnActionEvent;)V"

    // MethodIns
    // VarIns
    // FieldIns
    var ins = instruction
    val callAnActionUpdateMethod = ins.narrow<MethodInsnNode>() ?: return false
    ins = ins.previous
    val loadAnActionEventUpdateMethodParameter = ins.narrow<VarInsnNode>() ?: return false
    ins = ins.previous
    val getAnActionDelegateField = ins.narrow<FieldInsnNode>() ?: return false

    // field 'delegate': Lcom/intellij/openapi/actionSystem/AnAction;
    //    field delegate must be a subclass of `AnAction`
    // callAnActionUpdateMethod must be: name = 'update', desc = '(Lcom/intellij/openapi/actionSystem/AnActionEvent;)V'

    return false
  }

  private inline fun <reified T : AbstractInsnNode> AbstractInsnNode.narrow(): T? {
    return if (this is T) this else null
  }
}


