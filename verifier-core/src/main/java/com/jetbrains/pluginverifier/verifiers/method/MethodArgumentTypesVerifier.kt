package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.createMethodLocation
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescr
import com.jetbrains.pluginverifier.verifiers.resolveClassOrProblem
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class MethodArgumentTypesVerifier : MethodVerifier {
  override fun verify(clazz: ClassNode, method: MethodNode, ctx: VerificationContext) {
    val methodType = Type.getType(method.desc)
    val argumentTypes = methodType.argumentTypes
    for (type in argumentTypes) {
      val argDescr = type.descriptor.extractClassNameFromDescr() ?: continue
      ctx.resolveClassOrProblem(argDescr, clazz) { createMethodLocation(clazz, method) }
    }


  }
}
