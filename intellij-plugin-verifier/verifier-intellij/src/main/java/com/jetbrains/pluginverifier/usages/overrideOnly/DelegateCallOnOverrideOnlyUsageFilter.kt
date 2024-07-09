package com.jetbrains.pluginverifier.usages.overrideOnly

import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescriptor
import com.jetbrains.pluginverifier.verifiers.filter.ApiUsageFilter
import com.jetbrains.pluginverifier.verifiers.isSubclassOrSelf
import com.jetbrains.pluginverifier.verifiers.resolution.BinaryClassName
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.isCallOfSuperMethod
import com.jetbrains.pluginverifier.verifiers.resolution.matches
import com.jetbrains.pluginverifier.verifiers.resolution.searchParentOverrides
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.VarInsnNode

class DelegateCallOnOverrideOnlyUsageFilter : ApiUsageFilter {
  @Suppress("UNUSED_VARIABLE")
  override fun allow(invokedMethod: Method,
                     invocationInstruction: AbstractInsnNode,
                     callerMethod: Method,
                     context: VerificationContext): Boolean = with(context.classResolver) {
    if (!isInvokedMethodAllowed(callerMethod, invokedMethod, invocationInstruction)) {
      return false
    }

    val callMethod = invocationInstruction.narrow<MethodInsnNode>() ?: return false
    val loadMethodParameter = callMethod.previousInstruction<VarInsnNode>() ?: return false
    val getDelegateField = loadMethodParameter.previousInstruction<FieldInsnNode>() ?: return false

    val delegateBinaryClassName = getDelegateField.fieldClass ?: return false
    val delegateClassNode = when (val classResolution = resolveClass(delegateBinaryClassName)) {
      is ResolutionResult.Found<ClassNode> -> classResolution.value
      else -> return false
    }

    // Search for top-most class that holds a method in which the invocation occurs.
    // Such class is a top-most type of the delegate field.
    val callerMethodTopMostSuperClass = callerMethod.getSuperClassName(resolver = this)

    // field delegate must be a subclass of allowed class
    val isSubclass = isSubclassOrSelf(delegateClassNode.name, callerMethodTopMostSuperClass)
    if (!isSubclass) {
      return false
    }
    if (!isDelegateInvocationAllowed(callerMethod, callMethod)) {
      return false
    }
    return true
  }

  /**
   * Search for top-most class that declares a method specified by the receiver.
   * The receiver is by definition overriding the discovered method.
   *
   * @return the class name of the discovered class.
   */
  private fun Method.getSuperClassName(resolver: Resolver): BinaryClassName {
    val topMostClass = searchParentOverrides(resolver).lastOrNull() ?: return containingClassFile.name
    return topMostClass.klass.name
  }

  private fun isDelegateInvocationAllowed(callerMethod: Method, delegateMethodInvocationInstruction: MethodInsnNode): Boolean {
    return delegateMethodInvocationInstruction.matches(callerMethod)
  }

  private fun isInvokedMethodAllowed(
    callerMethod: Method,
    invokedMethod: Method,
    invocationInstruction: AbstractInsnNode
  ) = callerMethod.matches(invokedMethod)
    // static methods with @OverrideOnly do not make sense due to shadowing
    && !invocationInstruction.isStatic
    && !isCallOfSuperMethod(callerMethod, invokedMethod, invocationInstruction)

  private inline fun <reified T : AbstractInsnNode> AbstractInsnNode?.previousInstruction(): T? {
    return this?.previous?.narrow<T>()
  }

  private inline fun <reified T : AbstractInsnNode> AbstractInsnNode.narrow(): T? {
    return if (this is T) this else null
  }

  private val FieldInsnNode.fieldClass: BinaryClassName?
    get() = desc.extractClassNameFromDescriptor()

  private val AbstractInsnNode.isStatic: Boolean
    get() = opcode == Opcodes.INVOKESTATIC
}