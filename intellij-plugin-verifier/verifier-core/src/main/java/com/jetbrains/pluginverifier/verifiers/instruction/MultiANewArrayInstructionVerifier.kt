package com.jetbrains.pluginverifier.verifiers.instruction

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescriptor
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassChecked
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode

class MultiANewArrayInstructionVerifier : InstructionVerifier {
  override fun verify(method: Method, instructionNode: AbstractInsnNode, context: VerificationContext) {
    if (instructionNode !is MultiANewArrayInsnNode) return

    val className = instructionNode.desc.extractClassNameFromDescriptor() ?: return

    //During resolution of the symbolic reference to the class, array, or interface type,
    // any of the exceptions documented in §5.4.3.1 can be thrown.
    context.classResolver.resolveClassChecked(className, method, context)
  }
}
