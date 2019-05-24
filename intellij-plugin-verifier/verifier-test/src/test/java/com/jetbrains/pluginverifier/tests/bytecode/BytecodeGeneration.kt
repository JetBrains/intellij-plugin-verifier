package com.jetbrains.pluginverifier.tests.bytecode

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.util.TraceClassVisitor
import java.io.PrintWriter

fun ClassNode.printBytecode() {
  val printWriter = PrintWriter(System.out)
  val traceClassVisitor = TraceClassVisitor(printWriter)
  accept(traceClassVisitor)
}

fun ByteArray.createClassNode(): ClassNode {
  val classNode = ClassNode()
  val classReader = ClassReader(this)
  classReader.accept(classNode, 0)
  return classNode
}