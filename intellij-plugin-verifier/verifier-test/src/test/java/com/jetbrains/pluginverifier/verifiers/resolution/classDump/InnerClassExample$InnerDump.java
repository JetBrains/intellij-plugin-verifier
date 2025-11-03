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

public class InnerClassExample$InnerDump implements Opcodes {

    public static byte[] dump() throws Exception {

        ClassWriter classWriter = new ClassWriter(0);
        FieldVisitor fieldVisitor;
        RecordComponentVisitor recordComponentVisitor;
        MethodVisitor methodVisitor;
        AnnotationVisitor annotationVisitor0;

        classWriter.visit(V17, ACC_PUBLIC | ACC_SUPER, "com/jetbrains/test/InnerClassExample$Inner", null, "java/lang/Object", null);

        classWriter.visitSource("InnerClassExample.java", null);

        classWriter.visitNestHost("com/jetbrains/test/InnerClassExample");

        classWriter.visitInnerClass("com/jetbrains/test/InnerClassExample$Inner", "com/jetbrains/test/InnerClassExample", "Inner", ACC_PUBLIC);

        {
            fieldVisitor = classWriter.visitField(ACC_FINAL | ACC_SYNTHETIC, "this$0", "Lcom/jetbrains/test/InnerClassExample;", null, null);
            fieldVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "(Lcom/jetbrains/test/InnerClassExample;Ljava/lang/String;ILjava/lang/String;)V", null, null);
            {
                annotationVisitor0 = methodVisitor.visitTypeAnnotation(369098752, null, "Lorg/jetbrains/annotations/PropertyKey;", false);
                annotationVisitor0.visit("resourceBundle", "messages.Messages");
                annotationVisitor0.visitEnd();
            }
            {
                annotationVisitor0 = methodVisitor.visitTypeAnnotation(369229824, null, "Lorg/jetbrains/annotations/PropertyKey;", false);
                annotationVisitor0.visit("resourceBundle", "messages.Messages");
                annotationVisitor0.visitEnd();
            }
            methodVisitor.visitAnnotableParameterCount(3, false);
            {
                annotationVisitor0 = methodVisitor.visitParameterAnnotation(0, "Lorg/jetbrains/annotations/PropertyKey;", false);
                annotationVisitor0.visit("resourceBundle", "messages.Messages");
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
            methodVisitor.visitLineNumber(7, label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitFieldInsn(PUTFIELD, "com/jetbrains/test/InnerClassExample$Inner", "this$0", "Lcom/jetbrains/test/InnerClassExample;");
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            methodVisitor.visitInsn(RETURN);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLocalVariable("this", "Lcom/jetbrains/test/InnerClassExample$Inner;", null, label0, label1, 0);
            methodVisitor.visitLocalVariable("this$0", "Lcom/jetbrains/test/InnerClassExample;", null, label0, label1, 1);
            methodVisitor.visitLocalVariable("name", "Ljava/lang/String;", null, label0, label1, 2);
            methodVisitor.visitLocalVariable("x", "I", null, label0, label1, 3);
            methodVisitor.visitLocalVariable("name2", "Ljava/lang/String;", null, label0, label1, 4);
            methodVisitor.visitMaxs(2, 5);
            methodVisitor.visitEnd();
        }
        classWriter.visitEnd();

        return classWriter.toByteArray();
    }
}
