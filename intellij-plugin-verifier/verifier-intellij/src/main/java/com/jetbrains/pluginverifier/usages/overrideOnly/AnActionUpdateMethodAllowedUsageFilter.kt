package com.jetbrains.pluginverifier.usages.overrideOnly

import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescriptor
import com.jetbrains.pluginverifier.verifiers.filter.ApiUsageFilter
import com.jetbrains.pluginverifier.verifiers.isSubclassOrSelf
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.isCallOfSuperMethod
import org.objectweb.asm.tree.*


typealias BinaryClassName = String

class AnActionUpdateMethodAllowedUsageFilter : ApiUsageFilter {
  override fun allowMethodInvocation(methodReference: MethodReference,
                                     resolvedMethod: Method,
                                     instructionNode: AbstractInsnNode,
                                     callerMethod: Method,
                                     context: VerificationContext): Boolean {
    return isAnActionUpdateSuperCall(callerMethod, resolvedMethod, instructionNode)
      || isAnActionUpdateDelegateCall(resolvedMethod, instructionNode, context)
  }

  private fun isAnActionUpdateSuperCall(callerMethod: Method, resolvedMethod: Method, instructionNode: AbstractInsnNode): Boolean {
    return (resolvedMethod.isAnActionStyleUpdateMethod()
      && isCallOfSuperMethod(callerMethod, resolvedMethod, instructionNode))
  }

  @Suppress("UNUSED_VARIABLE")
  private fun isAnActionUpdateDelegateCall(resolvedMethod: Method,
                                           instruction: AbstractInsnNode,
                                           context: VerificationContext): Boolean = with(context.classResolver) {
    val isCallingAnActionUpdateMethod = resolvedMethod.isAnActionStyleUpdateMethod()
    if (!isCallingAnActionUpdateMethod) {
      return false
    }

    var ins = instruction
    val callAnActionUpdateMethod = ins.narrow<MethodInsnNode>() ?: return false
    ins = ins.previous
    val loadAnActionEventUpdateMethodParameter = ins.narrow<VarInsnNode>() ?: return false
    ins = ins.previous
    val getAnActionDelegateField = ins.narrow<FieldInsnNode>() ?: return false

    val delegateBinaryClassName = getAnActionDelegateField.fieldClass ?: return false
    val delegateClassNode = when (val classResolution = resolveClass(delegateBinaryClassName)) {
      is ResolutionResult.Found<ClassNode> -> classResolution.value
      else -> return false
    }
    // field delegate must be a subclass of `AnAction`
    val isSubclassOfAnAction = isSubclassOrSelf(delegateClassNode.name, "com/intellij/openapi/actionSystem/AnAction")
    if (!isSubclassOfAnAction) {
      return false
    }
    // callAnActionUpdateMethod must be: name = 'update', desc = '(Lcom/intellij/openapi/actionSystem/AnActionEvent;)V'
    if (!callAnActionUpdateMethod.isAnActionStyleUpdateMethod()) {
      return false
    }
    return true
  }

  private inline fun <reified T : AbstractInsnNode> AbstractInsnNode.narrow(): T? {
    return if (this is T) this else null
  }

  private val FieldInsnNode.fieldClass: BinaryClassName?
    get() = desc.extractClassNameFromDescriptor()

  private fun Method.isAnActionStyleUpdateMethod(): Boolean {
    return (name to descriptor).isAnActionStyleUpdateMethod()
  }

  private fun MethodInsnNode.isAnActionStyleUpdateMethod(): Boolean {
    return (name to desc).isAnActionStyleUpdateMethod()
  }

  private fun Pair<String, String>.isAnActionStyleUpdateMethod(): Boolean {
    return ("update" to "(Lcom/intellij/openapi/actionSystem/AnActionEvent;)V") == this
  }
}