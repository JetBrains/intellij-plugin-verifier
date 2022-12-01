/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.instruction

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescriptor
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassChecked
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.MultiANewArrayInsnNode

class MultiANewArrayInstructionVerifier : InstructionVerifier {
  override fun verify(method: Method, instructionNode: AbstractInsnNode, context: VerificationContext) {
    if (instructionNode !is MultiANewArrayInsnNode) return

    val className = instructionNode.desc.extractClassNameFromDescriptor() ?: return

    //During resolution of the symbolic reference to the class, array, or interface type,
    // any of the exceptions documented in §5.4.3.1 can be thrown.
    context.classResolver.resolveClassChecked(className, method, context)
  }
}
