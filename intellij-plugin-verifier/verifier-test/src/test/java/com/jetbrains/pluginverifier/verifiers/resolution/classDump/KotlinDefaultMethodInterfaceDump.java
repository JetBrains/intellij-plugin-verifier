/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.resolution.classDump;

import org.objectweb.asm.*;

/**
 * ASM dump of the Kotlin 2.2.0 (default JvmDefaultMode ENABLE) compiled output of:
 * <pre>
 * interface TestInterface {
 *     fun entryPoint() { internalMethod() }
 *     &#64;ApiStatus.Internal fun internalMethod() {}
 * }
 * </pre>
 */
public class KotlinDefaultMethodInterfaceDump implements Opcodes {

    public static byte[] dump() throws Exception {

        ClassWriter classWriter = new ClassWriter(0);
        FieldVisitor fieldVisitor;
        RecordComponentVisitor recordComponentVisitor;
        MethodVisitor methodVisitor;
        AnnotationVisitor annotationVisitor0;

        classWriter.visit(V17, ACC_PUBLIC | ACC_ABSTRACT | ACC_INTERFACE, "com/jetbrains/test/TestInterface", null, "java/lang/Object", null);

        classWriter.visitSource("Test.kt", null);

        {
            annotationVisitor0 = classWriter.visitAnnotation("Lkotlin/Metadata;", true);
            annotationVisitor0.visit("mv", new int[]{2, 2, 0});
            annotationVisitor0.visit("k", Integer.valueOf(1));
            annotationVisitor0.visit("xi", Integer.valueOf(48));
            annotationVisitor0.visitEnd();
        }
        classWriter.visitInnerClass("com/jetbrains/test/TestInterface$DefaultImpls", "com/jetbrains/test/TestInterface", "DefaultImpls", ACC_PUBLIC | ACC_FINAL | ACC_STATIC);

        classWriter.visitInnerClass("org/jetbrains/annotations/ApiStatus$Internal", "org/jetbrains/annotations/ApiStatus", "Internal", ACC_PUBLIC | ACC_STATIC | ACC_ANNOTATION | ACC_ABSTRACT | ACC_INTERFACE);

        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "entryPoint", "()V", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitLineNumber(7, label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "com/jetbrains/test/TestInterface", "internalMethod", "()V", true);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLineNumber(8, label1);
            methodVisitor.visitInsn(RETURN);
            Label label2 = new Label();
            methodVisitor.visitLabel(label2);
            methodVisitor.visitLocalVariable("this", "Lcom/jetbrains/test/TestInterface;", null, label0, label2, 0);
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
            methodVisitor.visitLineNumber(12, label0);
            methodVisitor.visitInsn(RETURN);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLocalVariable("this", "Lcom/jetbrains/test/TestInterface;", null, label0, label1, 0);
            methodVisitor.visitMaxs(0, 1);
            methodVisitor.visitEnd();
        }
        classWriter.visitEnd();

        return classWriter.toByteArray();
    }
}
