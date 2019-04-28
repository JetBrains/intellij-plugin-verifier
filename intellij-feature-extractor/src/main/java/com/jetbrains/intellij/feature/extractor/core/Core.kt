package com.jetbrains.intellij.feature.extractor.core

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.analysis.SourceValue
import org.objectweb.asm.tree.analysis.Value

fun MethodNode.isAbstract(): Boolean = this.access and Opcodes.ACC_ABSTRACT != 0

fun FieldNode.isStatic(): Boolean = this.access and Opcodes.ACC_STATIC != 0

fun ClassNode.findMethod(predicate: (MethodNode) -> Boolean): MethodNode? = findMethods(predicate).firstOrNull()

fun ClassNode.findMethods(predicate: (MethodNode) -> Boolean): List<MethodNode> = methods.filter(predicate)

fun ClassNode.findField(predicate: (FieldNode) -> Boolean): FieldNode? = fields.find(predicate)

fun MethodNode.instructionsAsList(): List<AbstractInsnNode> = instructions.toArray().toList()

fun Frame<SourceValue>.getOnStack(index: Int): Value? = this.getStack(this.stackSize - 1 - index)

inline fun <reified T> T.replicate(n: Int): List<T> = Array(n) { this }.toList()