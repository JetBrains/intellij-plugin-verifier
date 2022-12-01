package com.jetbrains.pluginverifier.tests.bytecode

import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.util.TraceClassVisitor
import java.io.PrintWriter
import java.io.StringWriter

fun ClassNode.printBytecode(): String {
  val stringWriter = StringWriter()
  val printWriter = PrintWriter(stringWriter)
  val traceClassVisitor = TraceClassVisitor(printWriter)
  accept(traceClassVisitor)
  return stringWriter.toString()
}

fun ByteArray.createClassNode(): ClassNode {
  val classNode = ClassNode()
  val classReader = ClassReader(this)
  classReader.accept(classNode, 0)
  return classNode
}