/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.resolution.classDump;

import org.objectweb.asm.*;

/**
 * ASM dump of the Kotlin 2.2.0 (default JvmDefaultMode ENABLE) compiled output of:
 * <pre>
 * interface AnnotatedDefaultMethodInterface {
 *     &#64;ApiStatus.Experimental fun experimentalMethod() {}
 *     &#64;Deprecated("Use something else") fun deprecatedMethod() {}
 * }
 * </pre>
 * The &#64;Internal counterpart is {@code KotlinDefaultMethodInterfaceDump}; this one covers the
 * experimental and deprecated usage detectors instead.
 * <p>
 * Paired with {@code KotlinAnnotatedDefaultMethodStubDump}, the implementor that doesn't override either method.
 * Synthetic {@code access$experimentalMethod$jd}/{@code access$deprecatedMethod$jd} accessors and the
 * {@code kotlin.Metadata} {@code d1}/{@code d2} payloads are elided as irrelevant to what this fixture tests.
 */
public class KotlinAnnotatedDefaultMethodInterfaceDump implements Opcodes {

    public static byte[] dump() throws Exception {

        ClassWriter classWriter = new ClassWriter(0);
        FieldVisitor fieldVisitor;
        RecordComponentVisitor recordComponentVisitor;
        MethodVisitor methodVisitor;
        AnnotationVisitor annotationVisitor0;

        classWriter.visit(V17, ACC_PUBLIC | ACC_ABSTRACT | ACC_INTERFACE, "com/jetbrains/test/AnnotatedDefaultMethodInterface", null, "java/lang/Object", null);

        classWriter.visitSource("Annotated.kt", null);

        {
            annotationVisitor0 = classWriter.visitAnnotation("Lkotlin/Metadata;", true);
            annotationVisitor0.visit("mv", new int[]{2, 2, 0});
            annotationVisitor0.visit("k", Integer.valueOf(1));
            annotationVisitor0.visit("xi", Integer.valueOf(48));
            annotationVisitor0.visitEnd();
        }
        classWriter.visitInnerClass("com/jetbrains/test/AnnotatedDefaultMethodInterface$DefaultImpls", "com/jetbrains/test/AnnotatedDefaultMethodInterface", "DefaultImpls", ACC_PUBLIC | ACC_FINAL | ACC_STATIC);

        classWriter.visitInnerClass("org/jetbrains/annotations/ApiStatus$Experimental", "org/jetbrains/annotations/ApiStatus", "Experimental", ACC_PUBLIC | ACC_STATIC | ACC_ANNOTATION | ACC_ABSTRACT | ACC_INTERFACE);

        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "experimentalMethod", "()V", null, null);
            {
                annotationVisitor0 = methodVisitor.visitAnnotation("Lorg/jetbrains/annotations/ApiStatus$Experimental;", false);
                annotationVisitor0.visitEnd();
            }
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitLineNumber(8, label0);
            methodVisitor.visitInsn(RETURN);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLocalVariable("this", "Lcom/jetbrains/test/AnnotatedDefaultMethodInterface;", null, label0, label1, 0);
            methodVisitor.visitMaxs(0, 1);
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
            methodVisitor.visitLineNumber(12, label0);
            methodVisitor.visitInsn(RETURN);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLocalVariable("this", "Lcom/jetbrains/test/AnnotatedDefaultMethodInterface;", null, label0, label1, 0);
            methodVisitor.visitMaxs(0, 1);
            methodVisitor.visitEnd();
        }
        classWriter.visitEnd();

        return classWriter.toByteArray();
    }
}
