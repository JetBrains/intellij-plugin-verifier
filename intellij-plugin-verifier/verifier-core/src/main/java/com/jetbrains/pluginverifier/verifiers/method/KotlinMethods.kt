package com.jetbrains.pluginverifier.verifiers.method

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.MethodInsnNode

object KotlinMethods {
  private const val CAPACITY = 100L

  private val cache: Cache<MethodLocation, Boolean> = Caffeine.newBuilder()
    .maximumSize(CAPACITY)
    .build()

  /**
   * Identify a Kotlin default method.
   *
   * A kotlin default method is a method that has a default implementation, but kotlin compiles
   * in the inheritor an override that calls the default implementation (inner) class known as
   * `DefaultImpls`.
   *
   * Ignoring labels, line numbers, and kotlin intrinsics (to check nullness), there's only a
   * handful of interesting bytecode that invoke the static method in `{TheInterfaceType}$DefaultImpls`
   * loading first `this`, then loading parameters if any.
   *
   * E.g. with
   *
   * ```
   * // access flags 0x1
   * public internalArgsReturningInternal(Linternal/defaultMethod/AnInternalType;Ljava/lang/String;)Linternal/defaultMethod/AnInternalType;
   *    L0
   *     ALOAD 1
   *     LDC "anInternalType"
   *     INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkNotNullParameter (Ljava/lang/Object;Ljava/lang/String;)V
   *     ALOAD 2
   *     LDC "s"
   *     INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkNotNullParameter (Ljava/lang/Object;Ljava/lang/String;)V
   *    L1
   *     LINENUMBER 5 L1
   *     ALOAD 0
   *     ALOAD 1
   *     ALOAD 2
   *     INVOKESTATIC internal/defaultMethod/InterfaceWithDefaultMethodUsingInternalAPI$DefaultImpls.internalArgsReturningInternal (Linternal/defaultMethod/InterfaceWithDefaultMethodUsingInternalAPI;Linternal/defaultMethod/AnInternalType;Ljava/lang/String;)Linternal/defaultMethod/AnInternalType;
   *     ARETURN
   *    L2
   *     LOCALVARIABLE this Linternal/defaultMethod/InterfaceWithDefaultMethodUsingInternalAPI; L0 L2 0
   *     LOCALVARIABLE anInternalType Linternal/defaultMethod/AnInternalType; L0 L2 1
   *     LOCALVARIABLE s Ljava/lang/String; L0 L2 2
   *     MAXSTACK = 3
   *     MAXLOCALS = 3
   * ```
   *
   * @see [`JvmDefault` annotation](https://github.com/JetBrains/kotlin/blob/master/libraries/stdlib/jvm/src/kotlin/jvm/JvmDefault.kt)
   */
  fun Method.isKotlinDefaultMethod(): Boolean {
    val method: Method = this
    return cache.asMap().computeIfAbsent(method.location) { isKotlinMethodInvokingDefaultImpls(method) }
  }

  private fun Method.isKotlinMethodInvokingDefaultImpls(method: Method): Boolean {
    // filter non-Kotlin classes
    if (!method.containingClassFile.annotations.any { it.desc == "Lkotlin/Metadata;" }) {
      return false
    }

    // If this method doesn't have any bytecode, it will be skipped
    if (instructions.isEmpty()) {
      return false
    }

    // Skip non-opcodes, and Kotlin intrinsics on arguments
    val candidateOpcodes = mutableListOf<AbstractInsnNode>()
    val instructionCount = instructions.size

    var i = 0
    while (i < instructionCount) {
      val instruction = instructions[i]
      if (instruction.opcode == -1) {
        i++
        continue
      }
      if (instruction.isKotlinIntrinsic()) {
        i += 3 // Skip ALOAD, LDC, and INVOKESTATIC
        continue
      }
      candidateOpcodes += instruction
      i++
    }

    val expectedOpcodes = (
      3 // aload this + invokestatic + (return or areturn)
        + method.methodParameters.size // aload for each parameter
      )

    if (candidateOpcodes.size != expectedOpcodes
      || candidateOpcodes[0].opcode != Opcodes.ALOAD // aload this
      || candidateOpcodes.slice(1..method.methodParameters.size).any() { it.opcode != Opcodes.ALOAD } // parameters
      || candidateOpcodes[candidateOpcodes.lastIndex - 1].opcode != Opcodes.INVOKESTATIC
      || candidateOpcodes.last().opcode !in intArrayOf(
        Opcodes.RETURN,
        Opcodes.ARETURN,
        Opcodes.DRETURN,
        Opcodes.FRETURN,
        Opcodes.IRETURN,
        Opcodes.LRETURN
      )
    ) {
      return false
    }

    val methodInsnNode = candidateOpcodes[candidateOpcodes.lastIndex - 1] as MethodInsnNode
    if (methodInsnNode.name != method.name || !methodInsnNode.owner.endsWith("\$DefaultImpls")) {
      return false
    }

    val actualKotlinOwner = methodInsnNode.owner.substringBeforeLast("\$DefaultImpls")

    // Walking the whole class hierarchy is not necessary because
    // these methods always delegate to the immediate superinterface. E.g.
    // ```
    // interface A {
    //    fun foo() {}
    // }
    //
    // interface B : A
    //
    // class C : B
    // ```
    // `C.foo` will have a call to `B$DefaultImpls.foo`.
    val isAParent = method.containingClassFile.interfaces.any {
      it == actualKotlinOwner
    }
    if (!isAParent) {
      return false
    }

    // The method is a kotlin default method
    return true
  }

  private fun AbstractInsnNode.isKotlinIntrinsic(): Boolean {
    return opcode == Opcodes.ALOAD
      && next?.opcode == Opcodes.LDC
      && next?.next?.opcode == Opcodes.INVOKESTATIC
      && (next?.next as MethodInsnNode).owner == "kotlin/jvm/internal/Intrinsics"
  }
}