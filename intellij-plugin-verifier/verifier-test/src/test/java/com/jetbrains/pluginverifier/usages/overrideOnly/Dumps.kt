@file:Suppress("TestFunctionName")

package com.jetbrains.pluginverifier.usages.overrideOnly

import com.jetbrains.pluginverifier.tests.bytecode.BinaryClassContent
import com.jetbrains.pluginverifier.tests.bytecode.createClassNode
import com.jetbrains.pluginverifier.verifiers.resolution.BinaryClassName
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode


internal fun ClearCountingContainerNode(): ClassNode = dumpClearCountingContainer().createClassNode()
internal fun ContainerNode(): ClassNode = dumpContainer().createClassNode()
internal fun PackageInvokingBoxNode(): ClassNode = dumpPackageInvokingBox().createClassNode()

private fun dumpClearCountingContainer(): ByteArray {
  val classWriter = ClassWriter(0)
  var fieldVisitor: FieldVisitor
  var methodVisitor: MethodVisitor

  classWriter.visit(
    V11,
    ACC_PUBLIC or ACC_SUPER,
    "mock/plugin/overrideOnly/ClearCountingContainer",
    null,
    "mock/plugin/overrideOnly/Container",
    null
  )

  classWriter.visitSource("ClearCountingContainer.java", null)

  run {
    fieldVisitor = classWriter.visitField(ACC_PRIVATE, "clearCount", "I", null, null)
    fieldVisitor.visitEnd()
  }
  run {
    methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null)
    methodVisitor.visitCode()
    val label0 = Label()
    methodVisitor.visitLabel(label0)
    methodVisitor.visitLineNumber(3, label0)
    methodVisitor.visitVarInsn(ALOAD, 0)
    methodVisitor.visitMethodInsn(INVOKESPECIAL, "mock/plugin/overrideOnly/Container", "<init>", "()V", false)
    val label1 = Label()
    methodVisitor.visitLabel(label1)
    methodVisitor.visitLineNumber(4, label1)
    methodVisitor.visitVarInsn(ALOAD, 0)
    methodVisitor.visitInsn(ICONST_0)
    methodVisitor.visitFieldInsn(PUTFIELD, "mock/plugin/overrideOnly/ClearCountingContainer", "clearCount", "I")
    methodVisitor.visitInsn(RETURN)
    val label2 = Label()
    methodVisitor.visitLabel(label2)
    methodVisitor.visitLocalVariable(
      "this",
      "Lmock/plugin/overrideOnly/ClearCountingContainer;",
      null,
      label0,
      label2,
      0
    )
    methodVisitor.visitMaxs(2, 1)
    methodVisitor.visitEnd()
  }
  run {
    methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "clear", "()V", null, null)
    methodVisitor.visitCode()
    val label0 = Label()
    methodVisitor.visitLabel(label0)
    methodVisitor.visitLineNumber(8, label0)
    methodVisitor.visitVarInsn(ALOAD, 0)
    methodVisitor.visitMethodInsn(INVOKESPECIAL, "mock/plugin/overrideOnly/Container", "clear", "()V", false)
    val label1 = Label()
    methodVisitor.visitLabel(label1)
    methodVisitor.visitLineNumber(9, label1)
    methodVisitor.visitVarInsn(ALOAD, 0)
    methodVisitor.visitInsn(DUP)
    methodVisitor.visitFieldInsn(GETFIELD, "mock/plugin/overrideOnly/ClearCountingContainer", "clearCount", "I")
    methodVisitor.visitInsn(ICONST_1)
    methodVisitor.visitInsn(IADD)
    methodVisitor.visitFieldInsn(PUTFIELD, "mock/plugin/overrideOnly/ClearCountingContainer", "clearCount", "I")
    val label2 = Label()
    methodVisitor.visitLabel(label2)
    methodVisitor.visitLineNumber(10, label2)
    methodVisitor.visitInsn(RETURN)
    val label3 = Label()
    methodVisitor.visitLabel(label3)
    methodVisitor.visitLocalVariable(
      "this",
      "Lmock/plugin/overrideOnly/ClearCountingContainer;",
      null,
      label0,
      label3,
      0
    )
    methodVisitor.visitMaxs(3, 1)
    methodVisitor.visitEnd()
  }
  classWriter.visitEnd()

  return classWriter.toByteArray()
}

private fun dumpContainer(): ByteArray {
  val classWriter = ClassWriter(0)
  var methodVisitor: MethodVisitor

  classWriter.visit(
    V11,
    ACC_PUBLIC or ACC_SUPER,
    "mock/plugin/overrideOnly/Container",
    null,
    "java/lang/Object",
    null
  )

  classWriter.visitSource("Container.java", null)

  run {
    methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null)
    methodVisitor.visitCode()
    val label0 = Label()
    methodVisitor.visitLabel(label0)
    methodVisitor.visitLineNumber(3, label0)
    methodVisitor.visitVarInsn(ALOAD, 0)
    methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
    methodVisitor.visitInsn(RETURN)
    val label1 = Label()
    methodVisitor.visitLabel(label1)
    methodVisitor.visitLocalVariable("this", "Lmock/plugin/overrideOnly/Container;", null, label0, label1, 0)
    methodVisitor.visitMaxs(1, 1)
    methodVisitor.visitEnd()
  }
  run {
    methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "clear", "()V", null, null)
    methodVisitor.visitCode()
    val label0 = Label()
    methodVisitor.visitLabel(label0)
    methodVisitor.visitLineNumber(6, label0)
    methodVisitor.visitInsn(RETURN)
    val label1 = Label()
    methodVisitor.visitLabel(label1)
    methodVisitor.visitLocalVariable("this", "Lmock/plugin/overrideOnly/Container;", null, label0, label1, 0)
    methodVisitor.visitMaxs(0, 1)
    methodVisitor.visitEnd()
  }
  classWriter.visitEnd()

  return classWriter.toByteArray()
}

private fun dumpPackageInvokingBox(): ByteArray {
  val classWriter = ClassWriter(0)
  var methodVisitor: MethodVisitor

  classWriter.visit(
    V11,
    ACC_PUBLIC or ACC_SUPER,
    "mock/plugin/overrideOnly/PackageInvokingBox",
    null,
    "java/lang/Object",
    null
  )

  classWriter.visitSource("PackageInvokingBox.java", null)

  run {
    methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null)
    methodVisitor.visitCode()
    val label0 = Label()
    methodVisitor.visitLabel(label0)
    methodVisitor.visitLineNumber(3, label0)
    methodVisitor.visitVarInsn(ALOAD, 0)
    methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
    methodVisitor.visitInsn(RETURN)
    val label1 = Label()
    methodVisitor.visitLabel(label1)
    methodVisitor.visitLocalVariable("this", "Lmock/plugin/overrideOnly/PackageInvokingBox;", null, label0, label1, 0)
    methodVisitor.visitMaxs(1, 1)
    methodVisitor.visitEnd()
  }
  run {
    methodVisitor =
      classWriter.visitMethod(ACC_PUBLIC, "getPackage", "(Ljava/lang/String;)Ljava/lang/Package;", null, null)
    methodVisitor.visitCode()
    val label0 = Label()
    methodVisitor.visitLabel(label0)
    methodVisitor.visitLineNumber(6, label0)
    methodVisitor.visitVarInsn(ALOAD, 1)
    methodVisitor.visitMethodInsn(
      INVOKESTATIC,
      "java/lang/Package",
      "getPackage",
      "(Ljava/lang/String;)Ljava/lang/Package;",
      false
    )
    methodVisitor.visitInsn(ARETURN)
    val label1 = Label()
    methodVisitor.visitLabel(label1)
    methodVisitor.visitLocalVariable(
      "this",
      "Lmock/plugin/overrideOnly/PackageInvokingBox;",
      null,
      label0,
      label1,
      0
    )
    methodVisitor.visitLocalVariable("pkg", "Ljava/lang/String;", null, label0, label1, 1)
    methodVisitor.visitMaxs(1, 2)
    methodVisitor.visitEnd()
  }
  classWriter.visitEnd()

  return classWriter.toByteArray()
}

/**
 * Simulates a situation when Java 17 builds a class which
 * uses `INVOKEINTERFACE` instruction on a lambda.
 *
 * The lambda compiles to a `private default synthetic` method,
 * and it is dynamically dispatched via `INVOKEDYNAMIC`.
 *
 * ```java
 * package mock;
 *
 * import java.util.function.Supplier;
 *
 * public interface Handler {
 *   default void handle() {
 *     Supplier<Integer> s = () -> getValue();
 *   }
 *
 *   default int getValue() {
 *     return 0;
 *   }
 * }
 * ```
 *
 * *Warning*: Java 8--17 might use `INVOKESPECIAL` instruction instead,
 * which is not covered by this dump.
 */
internal fun dumpInterfaceWithDefaultMethodsAndLambdas(): BinaryClassContent {
  val classWriter = ClassWriter(0)
  val className: BinaryClassName = "mock/Handler"
  classWriter.visit(
    V17,
    ACC_PUBLIC or ACC_ABSTRACT or ACC_INTERFACE,
    className,
    null,
    "java/lang/Object",
    null
  )

  classWriter.visitInnerClass(
    "java/lang/invoke/MethodHandles\$Lookup",
    "java/lang/invoke/MethodHandles",
    "Lookup",
    ACC_PUBLIC or ACC_FINAL or ACC_STATIC
  )

  classWriter.visitMethod(ACC_PUBLIC, "handle", "()V", null, null).run {
    visitCode()
    visitVarInsn(ALOAD, 0)
    visitInvokeDynamicInsn(
      "get",
      "(Lmock/plugin/interfaces/Handler;)Ljava/util/function/Supplier;",
      Handle(
        H_INVOKESTATIC,
        "java/lang/invoke/LambdaMetafactory",
        "metafactory",
        "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
        false
      ),
      Type.getType("()Ljava/lang/Object;"), Handle(
        H_INVOKEINTERFACE,
        className,
        "lambda\$handle$0",
        "()Ljava/lang/Integer;",
        true
      ), Type.getType("()Ljava/lang/Integer;")
    )
    visitVarInsn(ASTORE, 1)
    visitInsn(RETURN)
    visitMaxs(1, 2)
    visitEnd()
  }

  classWriter.visitMethod(ACC_PUBLIC, "getValue", "()I", null, null).run {
    visitCode()
    visitInsn(ICONST_0)
    visitInsn(IRETURN)
    visitMaxs(1, 1)
    visitEnd()
  }

  classWriter.visitMethod(ACC_PRIVATE or ACC_SYNTHETIC, "lambda\$handle$0", "()Ljava/lang/Integer;", null, null).run {
    visitCode()
    visitVarInsn(ALOAD, 0)
    visitMethodInsn(INVOKEINTERFACE, className, "getValue", "()I", true)
    visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)
    visitInsn(ARETURN)
    visitMaxs(1, 1)
    visitEnd()
  }

  classWriter.visitEnd()

  return BinaryClassContent(className, classWriter.toByteArray())
}
