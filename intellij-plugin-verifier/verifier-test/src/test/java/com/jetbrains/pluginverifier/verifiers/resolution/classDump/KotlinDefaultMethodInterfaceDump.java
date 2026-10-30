/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.resolution.classDump;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
/**
 * ASM dump of {@code TestInterface}, as compiled by Kotlin 2.4.10 with {@code -jvm-default=enable} from:
 * <pre>
 * package com.jetbrains.test
 *
 * import org.jetbrains.annotations.ApiStatus
 *
 * interface TestInterface {
 *   fun entryPoint() { internalMethod() }
 *   &#64;ApiStatus.Internal fun internalMethod() {}
 * }
 *
 * class TestImpl : TestInterface
 * </pre>
 */
public class KotlinDefaultMethodInterfaceDump implements Opcodes {

public static byte[] dump () throws Exception {

ClassWriter classWriter = new ClassWriter(0);
FieldVisitor fieldVisitor;
RecordComponentVisitor recordComponentVisitor;
MethodVisitor methodVisitor;
AnnotationVisitor annotationVisitor0;

classWriter.visit(V1_8, ACC_PUBLIC | ACC_ABSTRACT | ACC_INTERFACE, "com/jetbrains/test/TestInterface", null, "java/lang/Object", null);

classWriter.visitSource("Test.kt", null);

{
annotationVisitor0 = classWriter.visitAnnotation("Lkotlin/Metadata;", true);
annotationVisitor0.visit("mv", new int[] {2,4,0});
annotationVisitor0.visit("k", Integer.valueOf(1));
annotationVisitor0.visit("xi", Integer.valueOf(48));
{
AnnotationVisitor annotationVisitor1 = annotationVisitor0.visitArray("d1");
annotationVisitor1.visit(null, "\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\u0008f\u0018\u00002\u00020\u0001J\u0008\u0010\u0002\u001a\u00020\u0003H\u0016J\u000c\u0010\u0004\u001a\u00020\u0003H\u0017b\u0002\u0008\u0005\u00c0\u0006\u0003");
annotationVisitor1.visitEnd();
}
{
AnnotationVisitor annotationVisitor1 = annotationVisitor0.visitArray("d2");
annotationVisitor1.visit(null, "Lcom/jetbrains/test/TestInterface;");
annotationVisitor1.visit(null, "");
annotationVisitor1.visit(null, "entryPoint");
annotationVisitor1.visit(null, "");
annotationVisitor1.visit(null, "internalMethod");
annotationVisitor1.visit(null, "Lorg/jetbrains/annotations/ApiStatus$Internal;");
annotationVisitor1.visitEnd();
}
annotationVisitor0.visitEnd();
}
classWriter.visitInnerClass("com/jetbrains/test/TestInterface$DefaultImpls", "com/jetbrains/test/TestInterface", "DefaultImpls", ACC_PUBLIC | ACC_FINAL | ACC_STATIC);

classWriter.visitInnerClass("org/jetbrains/annotations/ApiStatus$Internal", "org/jetbrains/annotations/ApiStatus", "Internal", ACC_PUBLIC | ACC_STATIC | ACC_ANNOTATION | ACC_ABSTRACT | ACC_INTERFACE);

{
methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "entryPoint", "()V", null, null);
methodVisitor.visitCode();
Label label0 = new Label();
methodVisitor.visitLabel(label0);
methodVisitor.visitLineNumber(6, label0);
methodVisitor.visitVarInsn(ALOAD, 0);
methodVisitor.visitMethodInsn(INVOKEINTERFACE, "com/jetbrains/test/TestInterface", "internalMethod", "()V", true);
methodVisitor.visitInsn(RETURN);
Label label1 = new Label();
methodVisitor.visitLabel(label1);
methodVisitor.visitLocalVariable("this", "Lcom/jetbrains/test/TestInterface;", null, label0, label1, 0);
methodVisitor.visitMaxs(1, 1);
methodVisitor.visitEnd();
}
{
methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "internalMethod", "()V", null, null);
{
annotationVisitor0 = methodVisitor.visitAnnotation("Lorg/jetbrains/annotations/ApiStatus$Internal;", false);
annotationVisitor0.visitEnd();
}
methodVisitor.visitCode();
Label label0 = new Label();
methodVisitor.visitLabel(label0);
methodVisitor.visitLineNumber(7, label0);
methodVisitor.visitInsn(RETURN);
Label label1 = new Label();
methodVisitor.visitLabel(label1);
methodVisitor.visitLocalVariable("this", "Lcom/jetbrains/test/TestInterface;", null, label0, label1, 0);
methodVisitor.visitMaxs(0, 1);
methodVisitor.visitEnd();
}
{
methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC, "access$entryPoint$jd", "(Lcom/jetbrains/test/TestInterface;)V", null, null);
methodVisitor.visitCode();
Label label0 = new Label();
methodVisitor.visitLabel(label0);
methodVisitor.visitVarInsn(ALOAD, 0);
Label label1 = new Label();
methodVisitor.visitLabel(label1);
methodVisitor.visitLineNumber(5, label1);
methodVisitor.visitMethodInsn(INVOKESPECIAL, "com/jetbrains/test/TestInterface", "entryPoint", "()V", true);
methodVisitor.visitInsn(RETURN);
Label label2 = new Label();
methodVisitor.visitLabel(label2);
methodVisitor.visitLocalVariable("$this", "Lcom/jetbrains/test/TestInterface;", null, label0, label2, 0);
methodVisitor.visitMaxs(1, 1);
methodVisitor.visitEnd();
}
{
methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC, "access$internalMethod$jd", "(Lcom/jetbrains/test/TestInterface;)V", null, null);
methodVisitor.visitCode();
Label label0 = new Label();
methodVisitor.visitLabel(label0);
methodVisitor.visitVarInsn(ALOAD, 0);
Label label1 = new Label();
methodVisitor.visitLabel(label1);
methodVisitor.visitLineNumber(5, label1);
methodVisitor.visitMethodInsn(INVOKESPECIAL, "com/jetbrains/test/TestInterface", "internalMethod", "()V", true);
methodVisitor.visitInsn(RETURN);
Label label2 = new Label();
methodVisitor.visitLabel(label2);
methodVisitor.visitLocalVariable("$this", "Lcom/jetbrains/test/TestInterface;", null, label0, label2, 0);
methodVisitor.visitMaxs(1, 1);
methodVisitor.visitEnd();
}
classWriter.visitEnd();

return classWriter.toByteArray();
}
}
