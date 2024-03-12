package com.jetbrains.pluginverifier.usages.overrideOnly

import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescriptor
import com.jetbrains.pluginverifier.verifiers.filter.ApiUsageFilter
import com.jetbrains.pluginverifier.verifiers.isSubclassOrSelf
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.isCallOfSuperMethod
import org.objectweb.asm.tree.*

class OverrideOnlyMethodAllowedUsageFilter(private val allowedMethodDescriptor: MethodDescriptor,
                                           private val superclassName: BinaryClassName) : ApiUsageFilter {

  override fun allowMethodInvocation(invokedMethod: Method,
                                     invocationInstruction: AbstractInsnNode,
                                     callerMethod: Method,
                                     context: VerificationContext): Boolean {

    val superCall = isSuperCall(callerMethod, invokedMethod, invocationInstruction)

    val invocationPredicate = object : InvocationPredicate {
      override fun accept(caller: Method, callee: Method): Boolean {
        return callee.matches(allowedMethodDescriptor)
      }

      override fun accept(caller: Method, callee: MethodInsnNode): Boolean {
        return callee.matches(allowedMethodDescriptor)
      }
    }
    val delegateCall = isDelegateCall(callerMethod, invokedMethod, invocationInstruction, context, invocationPredicate)
    return superCall || delegateCall
  }

  private fun isSuperCall(callerMethod: Method, resolvedMethod: Method, instructionNode: AbstractInsnNode): Boolean {
    return (resolvedMethod.matches(allowedMethodDescriptor)
      && isCallOfSuperMethod(callerMethod, resolvedMethod, instructionNode))
  }

  @Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER")
  private fun isDelegateCall(callerMethod: Method,
                             invokedMethod: Method,
                             invocationInstruction: AbstractInsnNode,
                             context: VerificationContext,
                             invocationPredicate: InvocationPredicate
                             ): Boolean = with(context.classResolver) {
    val isCallingAllowedMethod = invocationPredicate.accept(callerMethod, invokedMethod)
    if (!isCallingAllowedMethod) {
      return false
    }

    var ins = invocationInstruction
    val callMethod = ins.narrow<MethodInsnNode>() ?: return false
    ins = ins.previous
    val loadMethodParameter = ins.narrow<VarInsnNode>() ?: return false
    ins = ins.previous
    val getDelegateField = ins.narrow<FieldInsnNode>() ?: return false

    val delegateBinaryClassName = getDelegateField.fieldClass ?: return false
    val delegateClassNode = when (val classResolution = resolveClass(delegateBinaryClassName)) {
      is ResolutionResult.Found<ClassNode> -> classResolution.value
      else -> return false
    }
    // field delegate must be a subclass of allowed class
    val isSubclass = isSubclassOrSelf(delegateClassNode.name, superclassName)
    if (!isSubclass) {
      return false
    }
    if (!invocationPredicate.accept(callerMethod, callMethod)) {
      return false
    }
    return true
  }

  private inline fun <reified T : AbstractInsnNode> AbstractInsnNode.narrow(): T? {
    return if (this is T) this else null
  }

  private val FieldInsnNode.fieldClass: BinaryClassName?
    get() = desc.extractClassNameFromDescriptor()

}

data class MethodDescriptor(private val methodName: String, private val descriptor: String)

fun Method.matches(methodDescriptor: MethodDescriptor): Boolean =
  MethodDescriptor(name, descriptor) == methodDescriptor

fun MethodInsnNode.matches(methodDescriptor: MethodDescriptor): Boolean =
  MethodDescriptor(name, desc) == methodDescriptor

interface InvocationPredicate {
  fun accept(caller: Method, callee: Method): Boolean = true
  fun accept(caller: Method, callee: MethodInsnNode): Boolean = true
}

