package com.jetbrains.pluginverifier.tests.mocks.asm

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode

/**
 * Returns a public zero-argument method returning `void` with no instructions.
 *
 * ```
 * public void handle() {
 *    return;
 * }
 * ```
 */
fun publicNoArgReturnVoid(methodName: String): MethodNode {
  return MethodNode().apply {
    access = ACC_PUBLIC
    name = methodName
    desc = "()V"
    // return;
    instructions.add(InsnNode(RETURN))
  }
}

fun constructorPublicNoArg(): MethodNode = MethodNode().apply {
  access = ACC_PUBLIC
  name = "<init>"
  desc = "()V"
  // load `this` on the stack
  instructions.add(VarInsnNode(ALOAD, 0))
  // invoke super()
  instructions.add(MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V"))
  // return nothing, but this is an constructor
  instructions.add(InsnNode(RETURN))
  maxStack = 1
  maxLocals = 1
}

fun constructorPublicString(): MethodNode = MethodNode().apply {
  access = ACC_PUBLIC
  name = "<init>"
  desc = "(Ljava/lang/String;)V"
  // load `this` on the stack
  instructions.add(VarInsnNode(ALOAD, 0))
  // invoke super()
  instructions.add(MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V"))
  // return nothing, but this is an constructor
  instructions.add(InsnNode(RETURN))
  maxStack = 1
  // `this` and `String` arg
  maxLocals = 2
}

