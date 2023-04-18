package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodInsnNode

object KotlinMethods {


  /**
   * Identify a Kotlin default method.
   *
   * A kotlin default method is a method that has a default implementation, but kotlin compiles
   * in the inhertor an override that calls the default implementation (inner) class known as
   * `DefaultImpls`.
   *
   * There's only 3 interesting bytecode, the rest are labels and line numbers.
   *
   * ```
   * // access flags 0x1
   * public getPlaceholderCollector()Linternal/defaultMethod/AnInternalType;
   *     @Lorg/jetbrains/annotations/Nullable;() // invisible
   *     L0
   *        LINENUMBER 5 L0
   *        ALOAD 0
   *        INVOKESTATIC internal/defaultMethod/InterfaceWithDefaultMethodUsingInternalAPI$DefaultImpls.getPlaceholderCollector (Linternal/defaultMethod/InterfaceWithDefaultMethodUsingInternalAPI;)Linternal/defaultMethod/AnInternalType;
   *        ARETURN
   *     L1
   *        LOCALVARIABLE this Linternal/defaultMethod/InterfaceWithDefaultMethodUsingInternalAPI; L0 L1 0
   *        MAXSTACK = 1
   *        MAXLOCALS = 1
   * ```
   */
  fun Method.isKotlinDefaultImpl(): Boolean {
    val method: Method = this
    // filter non kotlin classes
    if (!method.containingClassFile.annotations.any { it.desc == "Lkotlin/Metadata;" }) {
      return false
    }

    // check the only 3 opcodes in this method are
    val realOpcodes = method.instructions.filter { it.opcode != -1 } // label or line numbers don't have opcode
    if (realOpcodes.size != 3
      || realOpcodes[0].opcode != Opcodes.ALOAD
      || realOpcodes[1].opcode != Opcodes.INVOKESTATIC
      || realOpcodes[2].opcode != Opcodes.ARETURN
    ) {
      return false
    }

    val methodInsnNode = realOpcodes[1] as MethodInsnNode
    if (methodInsnNode.name != method.name || !methodInsnNode.owner.endsWith("\$DefaultImpls")) {
      return false
    }

    val actualKotlinOwner = methodInsnNode.owner.substring(0, methodInsnNode.owner.length - "\$DefaultImpls".length)

    // do we want to walk the whole class hierarchy?
    val isAParent = method.containingClassFile.interfaces.any {
      it == actualKotlinOwner
    }
    if (!isAParent) {
      return false
    }

    // The method is a kotlin default method
    return true
  }
}