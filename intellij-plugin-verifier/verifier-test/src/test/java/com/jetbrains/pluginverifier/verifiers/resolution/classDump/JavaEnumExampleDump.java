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

public class JavaEnumExampleDump implements Opcodes {

    public static byte[] dump() throws Exception {

        ClassWriter classWriter = new ClassWriter(0);
        FieldVisitor fieldVisitor;
        RecordComponentVisitor recordComponentVisitor;
        MethodVisitor methodVisitor;
        AnnotationVisitor annotationVisitor0;

        classWriter.visit(V17, ACC_FINAL | ACC_SUPER | ACC_ENUM, "com/jetbrains/test/JavaEnumExample", "Ljava/lang/Enum<Lcom/jetbrains/test/JavaEnumExample;>;", "java/lang/Enum", null);

        classWriter.visitSource("JavaEnumExample.java", null);

        {
            fieldVisitor = classWriter.visitField(ACC_PUBLIC | ACC_FINAL | ACC_STATIC | ACC_ENUM, "A", "Lcom/jetbrains/test/JavaEnumExample;", null, null);
            fieldVisitor.visitEnd();
        }
        {
            fieldVisitor = classWriter.visitField(ACC_PUBLIC | ACC_FINAL | ACC_STATIC | ACC_ENUM, "B", "Lcom/jetbrains/test/JavaEnumExample;", null, null);
            fieldVisitor.visitEnd();
        }
        {
            fieldVisitor = classWriter.visitField(ACC_PUBLIC | ACC_FINAL | ACC_STATIC | ACC_ENUM, "C", "Lcom/jetbrains/test/JavaEnumExample;", null, null);
            fieldVisitor.visitEnd();
        }
        {
            fieldVisitor = classWriter.visitField(ACC_FINAL, "displayName", "Ljava/lang/String;", null, null);
            {
                annotationVisitor0 = fieldVisitor.visitAnnotation("Lorg/jetbrains/annotations/NotNull;", false);
                annotationVisitor0.visitEnd();
            }
            {
                annotationVisitor0 = fieldVisitor.visitTypeAnnotation(318767104, null, "Lorg/jetbrains/annotations/NotNull;", false);
                annotationVisitor0.visitEnd();
            }
            fieldVisitor.visitEnd();
        }
        {
            fieldVisitor = classWriter.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC | ACC_SYNTHETIC, "$VALUES", "[Lcom/jetbrains/test/JavaEnumExample;", null, null);
            fieldVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "values", "()[Lcom/jetbrains/test/JavaEnumExample;", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitLineNumber(6, label0);
            methodVisitor.visitFieldInsn(GETSTATIC, "com/jetbrains/test/JavaEnumExample", "$VALUES", "[Lcom/jetbrains/test/JavaEnumExample;");
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "[Lcom/jetbrains/test/JavaEnumExample;", "clone", "()Ljava/lang/Object;", false);
            methodVisitor.visitTypeInsn(CHECKCAST, "[Lcom/jetbrains/test/JavaEnumExample;");
            methodVisitor.visitInsn(ARETURN);
            methodVisitor.visitMaxs(1, 0);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "valueOf", "(Ljava/lang/String;)Lcom/jetbrains/test/JavaEnumExample;", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitLineNumber(6, label0);
            methodVisitor.visitLdcInsn(Type.getType("Lcom/jetbrains/test/JavaEnumExample;"));
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Enum", "valueOf", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;", false);
            methodVisitor.visitTypeInsn(CHECKCAST, "com/jetbrains/test/JavaEnumExample");
            methodVisitor.visitInsn(ARETURN);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLocalVariable("name", "Ljava/lang/String;", null, label0, label1, 0);
            methodVisitor.visitMaxs(2, 1);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PRIVATE, "<init>", "(Ljava/lang/String;IILjava/lang/String;Ljava/lang/String;)V", "(ILjava/lang/String;Ljava/lang/String;)V", null);
            {
                annotationVisitor0 = methodVisitor.visitTypeAnnotation(369164288, null, "Lorg/jetbrains/annotations/PropertyKey;", false);
                annotationVisitor0.visit("resourceBundle", "messages.Messages");
                annotationVisitor0.visitEnd();
            }
            {
                annotationVisitor0 = methodVisitor.visitTypeAnnotation(369229824, null, "Lorg/jetbrains/annotations/NotNull;", false);
                annotationVisitor0.visitEnd();
            }
            {
                annotationVisitor0 = methodVisitor.visitTypeAnnotation(369229824, null, "Lorg/jetbrains/annotations/PropertyKey;", false);
                annotationVisitor0.visit("resourceBundle", "messages.Messages");
                annotationVisitor0.visitEnd();
            }
            methodVisitor.visitAnnotableParameterCount(3, false);
            {
                annotationVisitor0 = methodVisitor.visitParameterAnnotation(1, "Lorg/jetbrains/annotations/PropertyKey;", false);
                annotationVisitor0.visit("resourceBundle", "messages.Messages");
                annotationVisitor0.visitEnd();
            }
            {
                annotationVisitor0 = methodVisitor.visitParameterAnnotation(2, "Lorg/jetbrains/annotations/NotNull;", false);
                annotationVisitor0.visitEnd();
            }
            {
                annotationVisitor0 = methodVisitor.visitParameterAnnotation(2, "Lorg/jetbrains/annotations/PropertyKey;", false);
                annotationVisitor0.visit("resourceBundle", "messages.Messages");
                annotationVisitor0.visitEnd();
            }
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitLineNumber(16, label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitVarInsn(ILOAD, 2);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Enum", "<init>", "(Ljava/lang/String;I)V", false);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLineNumber(17, label1);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitVarInsn(ALOAD, 4);
            methodVisitor.visitInsn(ICONST_0);
            methodVisitor.visitTypeInsn(ANEWARRAY, "java/lang/Object");
            methodVisitor.visitMethodInsn(INVOKESTATIC, "com/jetbrains/test/ResourcesLoader", "getString", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", false);
            methodVisitor.visitFieldInsn(PUTFIELD, "com/jetbrains/test/JavaEnumExample", "displayName", "Ljava/lang/String;");
            Label label2 = new Label();
            methodVisitor.visitLabel(label2);
            methodVisitor.visitLineNumber(18, label2);
            methodVisitor.visitInsn(RETURN);
            Label label3 = new Label();
            methodVisitor.visitLabel(label3);
            methodVisitor.visitLocalVariable("this", "Lcom/jetbrains/test/JavaEnumExample;", null, label0, label3, 0);
            methodVisitor.visitLocalVariable("x", "I", null, label0, label3, 3);
            methodVisitor.visitLocalVariable("propertyKey", "Ljava/lang/String;", null, label0, label3, 4);
            methodVisitor.visitLocalVariable("propertyKey2", "Ljava/lang/String;", null, label0, label3, 5);
            methodVisitor.visitMaxs(3, 6);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC, "$values", "()[Lcom/jetbrains/test/JavaEnumExample;", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitLineNumber(6, label0);
            methodVisitor.visitInsn(ICONST_3);
            methodVisitor.visitTypeInsn(ANEWARRAY, "com/jetbrains/test/JavaEnumExample");
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitInsn(ICONST_0);
            methodVisitor.visitFieldInsn(GETSTATIC, "com/jetbrains/test/JavaEnumExample", "A", "Lcom/jetbrains/test/JavaEnumExample;");
            methodVisitor.visitInsn(AASTORE);
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitInsn(ICONST_1);
            methodVisitor.visitFieldInsn(GETSTATIC, "com/jetbrains/test/JavaEnumExample", "B", "Lcom/jetbrains/test/JavaEnumExample;");
            methodVisitor.visitInsn(AASTORE);
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitInsn(ICONST_2);
            methodVisitor.visitFieldInsn(GETSTATIC, "com/jetbrains/test/JavaEnumExample", "C", "Lcom/jetbrains/test/JavaEnumExample;");
            methodVisitor.visitInsn(AASTORE);
            methodVisitor.visitInsn(ARETURN);
            methodVisitor.visitMaxs(4, 0);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitLineNumber(7, label0);
            methodVisitor.visitTypeInsn(NEW, "com/jetbrains/test/JavaEnumExample");
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitLdcInsn("A");
            methodVisitor.visitInsn(ICONST_0);
            methodVisitor.visitInsn(ICONST_1);
            methodVisitor.visitLdcInsn("not.existing.key");
            methodVisitor.visitLdcInsn("existing.key");
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "com/jetbrains/test/JavaEnumExample", "<init>", "(Ljava/lang/String;IILjava/lang/String;Ljava/lang/String;)V", false);
            methodVisitor.visitFieldInsn(PUTSTATIC, "com/jetbrains/test/JavaEnumExample", "A", "Lcom/jetbrains/test/JavaEnumExample;");
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLineNumber(8, label1);
            methodVisitor.visitTypeInsn(NEW, "com/jetbrains/test/JavaEnumExample");
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitLdcInsn("B");
            methodVisitor.visitInsn(ICONST_1);
            methodVisitor.visitInsn(ICONST_2);
            methodVisitor.visitLdcInsn("existing.key");
            methodVisitor.visitLdcInsn("not.existing.key");
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "com/jetbrains/test/JavaEnumExample", "<init>", "(Ljava/lang/String;IILjava/lang/String;Ljava/lang/String;)V", false);
            methodVisitor.visitFieldInsn(PUTSTATIC, "com/jetbrains/test/JavaEnumExample", "B", "Lcom/jetbrains/test/JavaEnumExample;");
            Label label2 = new Label();
            methodVisitor.visitLabel(label2);
            methodVisitor.visitLineNumber(9, label2);
            methodVisitor.visitTypeInsn(NEW, "com/jetbrains/test/JavaEnumExample");
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitLdcInsn("C");
            methodVisitor.visitInsn(ICONST_2);
            methodVisitor.visitInsn(ICONST_3);
            methodVisitor.visitLdcInsn("existing.key");
            methodVisitor.visitLdcInsn("existing.key");
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "com/jetbrains/test/JavaEnumExample", "<init>", "(Ljava/lang/String;IILjava/lang/String;Ljava/lang/String;)V", false);
            methodVisitor.visitFieldInsn(PUTSTATIC, "com/jetbrains/test/JavaEnumExample", "C", "Lcom/jetbrains/test/JavaEnumExample;");
            Label label3 = new Label();
            methodVisitor.visitLabel(label3);
            methodVisitor.visitLineNumber(6, label3);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "com/jetbrains/test/JavaEnumExample", "$values", "()[Lcom/jetbrains/test/JavaEnumExample;", false);
            methodVisitor.visitFieldInsn(PUTSTATIC, "com/jetbrains/test/JavaEnumExample", "$VALUES", "[Lcom/jetbrains/test/JavaEnumExample;");
            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(7, 0);
            methodVisitor.visitEnd();
        }
        classWriter.visitEnd();

        return classWriter.toByteArray();
    }
}
