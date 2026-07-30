/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.resolution.classDump;

import org.objectweb.asm.*;

/**
 * ASM dump of {@code class AnnotatedDefaultMethodImpl : AnnotatedDefaultMethodInterface} (no override)
 * compiled the same way as {@code KotlinAnnotatedDefaultMethodInterfaceDump}. Both compatibility stubs
 * forward to the interface's default impl, so besides the "method overridden" false positive they also
 * trip the experimental and deprecated <em>invocation</em> detectors on the forwarding INVOKESPECIAL.
 * <p>
 * The {@code kotlin.Metadata} {@code d1}/{@code d2} payloads are elided as irrelevant to what this
 * fixture tests.
 */
public class KotlinAnnotatedDefaultMethodStubDump implements Opcodes {

    public static byte[] dump() throws Exception {

        ClassWriter classWriter = new ClassWriter(0);
        FieldVisitor fieldVisitor;
        RecordComponentVisitor recordComponentVisitor;
        MethodVisitor methodVisitor;
        AnnotationVisitor annotationVisitor0;

        classWriter.visit(V17, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, "com/jetbrains/test/AnnotatedDefaultMethodImpl", null, "java/lang/Object", new String[]{"com/jetbrains/test/AnnotatedDefaultMethodInterface"});

        classWriter.visitSource("Annotated.kt", null);

        {
            annotationVisitor0 = classWriter.visitAnnotation("Lkotlin/Metadata;", true);
            annotationVisitor0.visit("mv", new int[]{2, 2, 0});
            annotationVisitor0.visit("k", Integer.valueOf(1));
            annotationVisitor0.visit("xi", Integer.valueOf(48));
            annotationVisitor0.visitEnd();
        }
        classWriter.visitInnerClass("org/jetbrains/annotations/ApiStatus$Experimental", "org/jetbrains/annotations/ApiStatus", "Experimental", ACC_PUBLIC | ACC_STATIC | ACC_ANNOTATION | ACC_ABSTRACT | ACC_INTERFACE);

        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitLineNumber(15, label0);
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
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "experimentalMethod", "()V", null, null);
            {
                annotationVisitor0 = methodVisitor.visitAnnotation("Lorg/jetbrains/annotations/ApiStatus$Experimental;", false);
                annotationVisitor0.visitEnd();
            }
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitLineNumber(15, label0);
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
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_DEPRECATED, "deprecatedMethod", "()V", null, null);
            {
                annotationVisitor0 = methodVisitor.visitAnnotation("Lkotlin/Deprecated;", true);
                annotationVisitor0.visit("message", "Use something else");
                annotationVisitor0.visitEnd();
            }
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitLineNumber(15, label0);
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
