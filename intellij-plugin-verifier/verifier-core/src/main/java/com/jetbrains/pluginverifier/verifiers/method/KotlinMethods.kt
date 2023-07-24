/*
 * Copyright 2000-2023 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.jetbrains.pluginverifier.verifiers.method

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.VarInsnNode

object KotlinMethods {
  private const val CAPACITY = 100L

  private val cache: Cache<MethodLocation, Boolean> = CacheBuilder.newBuilder()
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
    if (
      // filter non kotlin classes
      !method.containingClassFile.annotations.any { it.desc == "Lkotlin/Metadata;" }
      // Sanity check: if the method does not have bytecode, or it's a constructor or class intializer
      // this heuristic cannot run
      || instructions.isEmpty()
      || method.isConstructor
      || method.isClassInitializer
      ) {
      return false
    }

    // skip non opcodes, and kotlin intrinsics on arguments
    val candidateOpcodes = mutableListOf<AbstractInsnNode>()
    var i = 0
    do {
      val currentInsnNode = instructions[i]
      if (currentInsnNode.opcode == -1) {
        continue
      }

      // Drop kotlin Intrinsics
      if (currentInsnNode.opcode == Opcodes.ALOAD
        && currentInsnNode.next?.opcode == Opcodes.LDC
        && (currentInsnNode.next?.next?.opcode == Opcodes.INVOKESTATIC
          && (currentInsnNode.next?.next as MethodInsnNode).owner == "kotlin/jvm/internal/Intrinsics")) {
        i += 2
        continue
      }

      candidateOpcodes.add(currentInsnNode)
    } while (++i < instructions.size)

    val isDefaultCallingDefaultOfParentInterface = isDefaultCallingDefaultOfParentInterface(method, candidateOpcodes) // checkcast this

    val expectedOpcodes = (
      if (isDefaultCallingDefaultOfParentInterface)
        4 // aload this + checkcast + invokestatic + (return or areturn)
          + method.methodParameters.size // aload for each parameter
          - 1 // Skip first parameter, it's `this` or the interface (this)
      else
        3 // aload this + invokestatic + (return or areturn)
          + method.methodParameters.size // aload for each parameter
      )

    val nonThisStartParameterIndex = if (isDefaultCallingDefaultOfParentInterface)
      2 // before: aload this + checkcast
    else
      1 // before: aload this

    if (candidateOpcodes.size != expectedOpcodes
      || candidateOpcodes[0].opcode != Opcodes.ALOAD // aload this
      || candidateOpcodes.slice(nonThisStartParameterIndex..method.methodParameters.size).any() { it.opcode != Opcodes.ALOAD } // parameters
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

    // If the current class is the default implementation of a child interface
    // then kotlin make it the inner class of 2 inner classes
    // * `ParentInterface`
    // * `ChildInterface`
    // There can be more than 2 inner classes if the interface extends multiple interfaces.
    val isImplementingAParentInterface = containingClassFile.innerClasses.size > 1
      && isDefaultCallingDefaultOfParentInterface

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
    @Suppress("RedundantIf") // for reading clarity
    if (!isAParent && !isImplementingAParentInterface) {
      return false
    }

    // The method is a kotlin default method
    return true
  }

  /**
   * Check when an interface extends another the Idea interface that has private types.
   *
   * In this case, Kotlin generates a default implementation `MyInterface$DefaultImpls`
   * that calls the `ParentInterface$DefaultImpls` of the parent interface
   *
   * ```
   * // access flags 0x9
   * public static topInternal(Lmock/plugin/internal/defaultMethod/NoInternalTypeUsageInterface;)Linternal/defaultMethod/AnInternalType;
   * @Lorg/jetbrains/annotations/Nullable;() // invisible
   *   // annotable parameter count: 1 (invisible)
   *   @Lorg/jetbrains/annotations/NotNull;() // invisible, parameter 0
   *  L0
   *   LINENUMBER 5 L0
   *   ALOAD 0
   *   CHECKCAST internal/defaultMethod/InterfaceWithDefaultMethodUsingInternalAPI
   *   INVOKESTATIC internal/defaultMethod/InterfaceWithDefaultMethodUsingInternalAPI$DefaultImpls.topInternal (Linternal/defaultMethod/InterfaceWithDefaultMethodUsingInternalAPI;)Linternal/defaultMethod/AnInternalType;
   *  L1
   *   LINENUMBER 11 L1
   *   ARETURN
   *  L2
   *   LOCALVARIABLE $this Lmock/plugin/internal/defaultMethod/NoInternalTypeUsageInterface; L0 L2 0
   *   MAXSTACK = 1
   *   MAXLOCALS = 1
   * ```
   */
  private fun isDefaultCallingDefaultOfParentInterface(method: Method, candidateOpcodes: MutableList<AbstractInsnNode>) =
    method.isStatic
      && candidateOpcodes[0].opcode == Opcodes.ALOAD
      && (candidateOpcodes[0] as VarInsnNode).`var` == 0 // aload 0
      && candidateOpcodes[1].opcode == Opcodes.CHECKCAST
       && method.containingClassFile.innerClasses.any {
         (candidateOpcodes[1] as TypeInsnNode).desc == it.outerName
       }
}
