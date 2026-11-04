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
 * ASM dump of {@code AnnotatedDefaultMethodImpl}, as compiled by Kotlin 2.4.10 with {@code -jvm-default=enable} from:
 * <pre>
 * package com.jetbrains.test
 *
 * import org.jetbrains.annotations.ApiStatus
 *
 * interface AnnotatedDefaultMethodInterface {
 *   &#64;ApiStatus.Experimental fun experimentalMethod() {}
 *   &#64;Deprecated("Use something else") fun deprecatedMethod() {}
 * }
 *
 * class AnnotatedDefaultMethodImpl : AnnotatedDefaultMethodInterface
 * </pre>
 */
public class KotlinAnnotatedDefaultMethodStubDump implements Opcodes {

public static byte[] dump () throws Exception {

ClassWriter classWriter = new ClassWriter(0);
FieldVisitor fieldVisitor;
RecordComponentVisitor recordComponentVisitor;
MethodVisitor methodVisitor;
AnnotationVisitor annotationVisitor0;

classWriter.visit(V1_8, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, "com/jetbrains/test/AnnotatedDefaultMethodImpl", null, "java/lang/Object", new String[] { "com/jetbrains/test/AnnotatedDefaultMethodInterface" });

classWriter.visitSource("Annotated.kt", null);

{
annotationVisitor0 = classWriter.visitAnnotation("Lkotlin/Metadata;", true);
annotationVisitor0.visit("mv", new int[] {2,4,0});
annotationVisitor0.visit("k", Integer.valueOf(1));
annotationVisitor0.visit("xi", Integer.valueOf(48));
{
AnnotationVisitor annotationVisitor1 = annotationVisitor0.visitArray("d1");
annotationVisitor1.visit(null, "\u0000\u000c\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0008\u0002\u0018\u00002\u00020\u0001B\u0007\u00a2\u0006\u0004\u0008\u0002\u0010\u0003");
annotationVisitor1.visitEnd();
}
{
AnnotationVisitor annotationVisitor1 = annotationVisitor0.visitArray("d2");
annotationVisitor1.visit(null, "Lcom/jetbrains/test/AnnotatedDefaultMethodImpl;");
annotationVisitor1.visit(null, "Lcom/jetbrains/test/AnnotatedDefaultMethodInterface;");
annotationVisitor1.visit(null, "<init>");
annotationVisitor1.visit(null, "()V");
annotationVisitor1.visitEnd();
}
annotationVisitor0.visitEnd();
}
classWriter.visitInnerClass("org/jetbrains/annotations/ApiStatus$Experimental", "org/jetbrains/annotations/ApiStatus", "Experimental", ACC_PUBLIC | ACC_STATIC | ACC_ANNOTATION | ACC_ABSTRACT | ACC_INTERFACE);

{
methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
methodVisitor.visitCode();
Label label0 = new Label();
methodVisitor.visitLabel(label0);
methodVisitor.visitLineNumber(10, label0);
methodVisitor.visitVarInsn(ALOAD, 0);
methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
methodVisitor.visitInsn(RETURN);
Label label1 = new Label();
methodVisitor.visitLabel(label1);
methodVisitor.visitLocalVariable("this", "Lcom/jetbrains/test/AnnotatedDefaultMethodImpl;", null, label0, label1, 0);
methodVisitor.visitMaxs(1, 1);
methodVisitor.visitEnd();
}
{
methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_BRIDGE, "experimentalMethod", "()V", null, null);
{
annotationVisitor0 = methodVisitor.visitAnnotation("Lorg/jetbrains/annotations/ApiStatus$Experimental;", false);
annotationVisitor0.visitEnd();
}
methodVisitor.visitCode();
Label label0 = new Label();
methodVisitor.visitLabel(label0);
methodVisitor.visitLineNumber(10, label0);
methodVisitor.visitVarInsn(ALOAD, 0);
methodVisitor.visitMethodInsn(INVOKESPECIAL, "com/jetbrains/test/AnnotatedDefaultMethodInterface", "experimentalMethod", "()V", true);
methodVisitor.visitInsn(RETURN);
Label label1 = new Label();
methodVisitor.visitLabel(label1);
methodVisitor.visitLocalVariable("this", "Lcom/jetbrains/test/AnnotatedDefaultMethodImpl;", null, label0, label1, 0);
methodVisitor.visitMaxs(1, 1);
methodVisitor.visitEnd();
}
{
methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_BRIDGE | ACC_DEPRECATED, "deprecatedMethod", "()V", null, null);
{
annotationVisitor0 = methodVisitor.visitAnnotation("Lkotlin/Deprecated;", true);
annotationVisitor0.visit("message", "Use something else");
annotationVisitor0.visitEnd();
}
methodVisitor.visitCode();
Label label0 = new Label();
methodVisitor.visitLabel(label0);
methodVisitor.visitLineNumber(10, label0);
methodVisitor.visitVarInsn(ALOAD, 0);
methodVisitor.visitMethodInsn(INVOKESPECIAL, "com/jetbrains/test/AnnotatedDefaultMethodInterface", "deprecatedMethod", "()V", true);
methodVisitor.visitInsn(RETURN);
Label label1 = new Label();
methodVisitor.visitLabel(label1);
methodVisitor.visitLocalVariable("this", "Lcom/jetbrains/test/AnnotatedDefaultMethodImpl;", null, label0, label1, 0);
methodVisitor.visitMaxs(1, 1);
methodVisitor.visitEnd();
}
classWriter.visitEnd();

return classWriter.toByteArray();
}
}
