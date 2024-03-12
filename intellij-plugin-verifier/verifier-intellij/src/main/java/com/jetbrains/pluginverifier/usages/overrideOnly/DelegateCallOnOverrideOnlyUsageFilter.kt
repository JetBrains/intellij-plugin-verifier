package com.jetbrains.pluginverifier.usages.overrideOnly

import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescriptor
import com.jetbrains.pluginverifier.verifiers.filter.ApiUsageFilter
import com.jetbrains.pluginverifier.verifiers.isSubclassOrSelf
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.tree.*

class DelegateCallOnOverrideOnlyUsageFilter : ApiUsageFilter {
  //FIXME novotnyr remove when autodetect is enabled
  private val superclassName: String = anActionClass

  override fun allowMethodInvocation(invokedMethod: Method,
                                     invocationInstruction: AbstractInsnNode,
                                     callerMethod: Method,
                                     context: VerificationContext): Boolean = with(context.classResolver) {
    val isCallingAllowedMethod = isInvokedMethodAllowed(callerMethod, invokedMethod)
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
    if (!isDelegateInvocationAllowed(callerMethod, callMethod)) {
      return false
    }
    return true
  }

  private fun isDelegateInvocationAllowed(callerMethod: Method, delegateMethodInvocationInstruction: MethodInsnNode): Boolean {
    return callerMethod.name == delegateMethodInvocationInstruction.name &&
      callerMethod.descriptor == delegateMethodInvocationInstruction.desc
  }

  private fun isInvokedMethodAllowed(callerMethod: Method, invokedMethod: Method): Boolean {
    return callerMethod.matches(invokedMethod)
  }

  private inline fun <reified T : AbstractInsnNode> AbstractInsnNode.narrow(): T? {
    return if (this is T) this else null
  }

  private val FieldInsnNode.fieldClass: BinaryClassName?
    get() = desc.extractClassNameFromDescriptor()
}