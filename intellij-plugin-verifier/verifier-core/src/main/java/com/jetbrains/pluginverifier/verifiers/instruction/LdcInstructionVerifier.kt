/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.instruction

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescriptor
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassChecked
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.LdcInsnNode

class LdcInstructionVerifier : InstructionVerifier {
  override fun verify(method: Method, instructionNode: AbstractInsnNode, context: VerificationContext) {
    if (instructionNode !is LdcInsnNode) return

    val type = instructionNode.cst as? Type ?: return
    if (type.sort == Type.OBJECT) {
      checkTypeExists(type, context, method)
    } else if (type.sort == Type.METHOD) {
      for (argumentType in type.argumentTypes) {
        checkTypeExists(argumentType, context, method)
      }
      checkTypeExists(type.returnType, context, method)
    }
  }

  private fun checkTypeExists(type: Type, context: VerificationContext, method: Method) {
    val className = type.descriptor.extractClassNameFromDescriptor() ?: return
    context.classResolver.resolveClassChecked(className, method, context)
  }
}
