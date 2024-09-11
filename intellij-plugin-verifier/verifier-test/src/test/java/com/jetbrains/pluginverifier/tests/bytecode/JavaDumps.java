package com.jetbrains.pluginverifier.tests.bytecode;

import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.*;


public class JavaDumps {
    /**
     * Binary dump when using
     * <ul>
     *     <li>org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1"</li>
     *     <li>Kotlin JSON Serialization including Gradle plugin</li>
     * </ul>
     * <pre>
     * {@code
     import kotlinx.serialization.Serializable
     import kotlinx.serialization.json.JsonElement

     @Serializable
     data class Version(
     var version: String = "",
     var revision: String = "",
     var metadata: Map<String, JsonElement> = mapOf(),
     var advanced: Boolean? = false
     )
    }
     </pre>
     * @return
     * @throws Exception
     */
    public static byte[] getSerializableVersion() throws Exception {

        ClassWriter classWriter = new ClassWriter(0);
        FieldVisitor fieldVisitor;
        MethodVisitor methodVisitor;
        AnnotationVisitor annotationVisitor0;

        classWriter.visit(Opcodes.V17, ACC_PUBLIC | ACC_FINAL | ACC_SUPER | ACC_SYNTHETIC | ACC_DEPRECATED, "plugin/Version$$serializer", "Ljava/lang/Object;Lkotlinx/serialization/internal/GeneratedSerializer<Lplugin/Version;>;", "java/lang/Object", new String[]{"kotlinx/serialization/internal/GeneratedSerializer"});

        {
            annotationVisitor0 = classWriter.visitAnnotation("Lkotlin/Deprecated;", true);
            annotationVisitor0.visit("message", "This synthesized declaration should not be used directly");
            annotationVisitor0.visitEnum("level", "Lkotlin/DeprecationLevel;", "HIDDEN");
            annotationVisitor0.visitEnd();
        }
        {
            annotationVisitor0 = classWriter.visitAnnotation("Lkotlin/Metadata;", true);
            annotationVisitor0.visit("mv", new int[]{2, 0, 0});
            annotationVisitor0.visit("k", 1);
            annotationVisitor0.visit("xi", 48);
            {
                AnnotationVisitor annotationVisitor1 = annotationVisitor0.visitArray("d1");
                annotationVisitor1.visit(null, "\u00006\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0008\u0003\n\u0002\u0010\u0011\n\u0002\u0018\u0002\n\u0002\u0008\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0008\u0002\n\u0002\u0018\u0002\n\u0002\u0008\u0003\u0008\u00c7\u0002\u0018\u00002\u0008\u0012\u0004\u0012\u00020\u00020\u0001B\u0009\u0008\u0002\u00a2\u0006\u0004\u0008\u0003\u0010\u0004J\u0015\u0010\u0005\u001a\u000c\u0012\u0008\u0012\u0006\u0012\u0002\u0008\u00030\u00070\u0006\u00a2\u0006\u0002\u0010\u0008J\u000e\u0010\u0009\u001a\u00020\u00022\u0006\u0010\n\u001a\u00020\u000bJ\u0016\u0010\u000c\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0002R\u0011\u0010\u0011\u001a\u00020\u0012\u00a2\u0006\u0008\n\u0000\u001a\u0004\u0008\u0013\u0010\u0014\u00a8\u0006\u0015");
                annotationVisitor1.visitEnd();
            }
            {
                AnnotationVisitor annotationVisitor1 = annotationVisitor0.visitArray("d2");
                annotationVisitor1.visit(null, "plugin/Version.$serializer");
                annotationVisitor1.visit(null, "Lkotlinx/serialization/internal/GeneratedSerializer;");
                annotationVisitor1.visit(null, "Lplugin/Version;");
                annotationVisitor1.visit(null, "<init>");
                annotationVisitor1.visit(null, "()V");
                annotationVisitor1.visit(null, "childSerializers");
                annotationVisitor1.visit(null, "");
                annotationVisitor1.visit(null, "Lkotlinx/serialization/KSerializer;");
                annotationVisitor1.visit(null, "()[Lkotlinx/serialization/KSerializer;");
                annotationVisitor1.visit(null, "deserialize");
                annotationVisitor1.visit(null, "decoder");
                annotationVisitor1.visit(null, "Lkotlinx/serialization/encoding/Decoder;");
                annotationVisitor1.visit(null, "serialize");
                annotationVisitor1.visit(null, "");
                annotationVisitor1.visit(null, "encoder");
                annotationVisitor1.visit(null, "Lkotlinx/serialization/encoding/Encoder;");
                annotationVisitor1.visit(null, "value");
                annotationVisitor1.visit(null, "descriptor");
                annotationVisitor1.visit(null, "Lkotlinx/serialization/descriptors/SerialDescriptor;");
                annotationVisitor1.visit(null, "getDescriptor");
                annotationVisitor1.visit(null, "()Lkotlinx/serialization/descriptors/SerialDescriptor;");
                annotationVisitor1.visit(null, "kotlin-plugin-idea-plugin");
                annotationVisitor1.visitEnd();
            }
            annotationVisitor0.visitEnd();
        }
        classWriter.visitInnerClass("plugin/Version$$serializer", "plugin/Version", "$serializer", ACC_PUBLIC | ACC_SYNTHETIC);

        classWriter.visitInnerClass("kotlinx/serialization/internal/GeneratedSerializer$DefaultImpls", "kotlinx/serialization/internal/GeneratedSerializer", "DefaultImpls", ACC_PUBLIC | ACC_FINAL | ACC_STATIC);

        {
            fieldVisitor = classWriter.visitField(ACC_PUBLIC | ACC_FINAL | ACC_STATIC, "INSTANCE", "Lplugin/Version$$serializer;", null, null);
            {
                annotationVisitor0 = fieldVisitor.visitAnnotation("Lorg/jetbrains/annotations/NotNull;", false);
                annotationVisitor0.visitEnd();
            }
            fieldVisitor.visitEnd();
        }
        {
            fieldVisitor = classWriter.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, "descriptor", "Lkotlinx/serialization/descriptors/SerialDescriptor;", null, null);
            {
                annotationVisitor0 = fieldVisitor.visitAnnotation("Lorg/jetbrains/annotations/NotNull;", false);
                annotationVisitor0.visitEnd();
            }
            fieldVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PRIVATE, "<init>", "()V", null, null);
            methodVisitor.visitCode();
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(1, 1);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_FINAL, "serialize", "(Lkotlinx/serialization/encoding/Encoder;Lplugin/Version;)V", null, null);
            methodVisitor.visitAnnotableParameterCount(2, false);
            {
                annotationVisitor0 = methodVisitor.visitParameterAnnotation(0, "Lorg/jetbrains/annotations/NotNull;", false);
                annotationVisitor0.visitEnd();
            }
            {
                annotationVisitor0 = methodVisitor.visitParameterAnnotation(1, "Lorg/jetbrains/annotations/NotNull;", false);
                annotationVisitor0.visitEnd();
            }
            methodVisitor.visitCode();
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitLdcInsn("encoder");
            methodVisitor.visitMethodInsn(INVOKESTATIC, "kotlin/jvm/internal/Intrinsics", "checkNotNullParameter", "(Ljava/lang/Object;Ljava/lang/String;)V", false);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitLdcInsn("value");
            methodVisitor.visitMethodInsn(INVOKESTATIC, "kotlin/jvm/internal/Intrinsics", "checkNotNullParameter", "(Ljava/lang/Object;Ljava/lang/String;)V", false);
            methodVisitor.visitFieldInsn(GETSTATIC, "plugin/Version$$serializer", "descriptor", "Lkotlinx/serialization/descriptors/SerialDescriptor;");
            methodVisitor.visitVarInsn(ASTORE, 3);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitVarInsn(ALOAD, 3);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "kotlinx/serialization/encoding/Encoder", "beginStructure", "(Lkotlinx/serialization/descriptors/SerialDescriptor;)Lkotlinx/serialization/encoding/CompositeEncoder;", true);
            methodVisitor.visitVarInsn(ASTORE, 4);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitVarInsn(ALOAD, 4);
            methodVisitor.visitVarInsn(ALOAD, 3);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "plugin/Version", "write$Self$kotlin_plugin_idea_plugin", "(Lplugin/Version;Lkotlinx/serialization/encoding/CompositeEncoder;Lkotlinx/serialization/descriptors/SerialDescriptor;)V", false);
            methodVisitor.visitVarInsn(ALOAD, 4);
            methodVisitor.visitVarInsn(ALOAD, 3);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "kotlinx/serialization/encoding/CompositeEncoder", "endStructure", "(Lkotlinx/serialization/descriptors/SerialDescriptor;)V", true);
            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(3, 5);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_FINAL, "deserialize", "(Lkotlinx/serialization/encoding/Decoder;)Lplugin/Version;", null, null);
            {
                annotationVisitor0 = methodVisitor.visitAnnotation("Lorg/jetbrains/annotations/NotNull;", false);
                annotationVisitor0.visitEnd();
            }
            methodVisitor.visitAnnotableParameterCount(1, false);
            {
                annotationVisitor0 = methodVisitor.visitParameterAnnotation(0, "Lorg/jetbrains/annotations/NotNull;", false);
                annotationVisitor0.visitEnd();
            }
            methodVisitor.visitCode();
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitLdcInsn("decoder");
            methodVisitor.visitMethodInsn(INVOKESTATIC, "kotlin/jvm/internal/Intrinsics", "checkNotNullParameter", "(Ljava/lang/Object;Ljava/lang/String;)V", false);
            methodVisitor.visitFieldInsn(GETSTATIC, "plugin/Version$$serializer", "descriptor", "Lkotlinx/serialization/descriptors/SerialDescriptor;");
            methodVisitor.visitVarInsn(ASTORE, 2);
            methodVisitor.visitInsn(ICONST_1);
            methodVisitor.visitVarInsn(ISTORE, 3);
            methodVisitor.visitInsn(ICONST_0);
            methodVisitor.visitVarInsn(ISTORE, 5);
            methodVisitor.visitInsn(ACONST_NULL);
            methodVisitor.visitVarInsn(ASTORE, 6);
            methodVisitor.visitInsn(ACONST_NULL);
            methodVisitor.visitVarInsn(ASTORE, 7);
            methodVisitor.visitInsn(ACONST_NULL);
            methodVisitor.visitVarInsn(ASTORE, 8);
            methodVisitor.visitInsn(ACONST_NULL);
            methodVisitor.visitVarInsn(ASTORE, 9);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "kotlinx/serialization/encoding/Decoder", "beginStructure", "(Lkotlinx/serialization/descriptors/SerialDescriptor;)Lkotlinx/serialization/encoding/CompositeDecoder;", true);
            methodVisitor.visitVarInsn(ASTORE, 10);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "plugin/Version", "access$get$childSerializers$cp", "()[Lkotlinx/serialization/KSerializer;", false);
            methodVisitor.visitVarInsn(ASTORE, 11);
            methodVisitor.visitVarInsn(ALOAD, 10);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "kotlinx/serialization/encoding/CompositeDecoder", "decodeSequentially", "()Z", true);
            Label label0 = new Label();
            methodVisitor.visitJumpInsn(IFEQ, label0);
            methodVisitor.visitVarInsn(ALOAD, 10);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitInsn(ICONST_0);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "kotlinx/serialization/encoding/CompositeDecoder", "decodeStringElement", "(Lkotlinx/serialization/descriptors/SerialDescriptor;I)Ljava/lang/String;", true);
            methodVisitor.visitVarInsn(ASTORE, 6);
            methodVisitor.visitVarInsn(ILOAD, 5);
            methodVisitor.visitInsn(ICONST_1);
            methodVisitor.visitInsn(IOR);
            methodVisitor.visitVarInsn(ISTORE, 5);
            methodVisitor.visitVarInsn(ALOAD, 10);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitInsn(ICONST_1);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "kotlinx/serialization/encoding/CompositeDecoder", "decodeStringElement", "(Lkotlinx/serialization/descriptors/SerialDescriptor;I)Ljava/lang/String;", true);
            methodVisitor.visitVarInsn(ASTORE, 7);
            methodVisitor.visitVarInsn(ILOAD, 5);
            methodVisitor.visitInsn(ICONST_2);
            methodVisitor.visitInsn(IOR);
            methodVisitor.visitVarInsn(ISTORE, 5);
            methodVisitor.visitVarInsn(ALOAD, 10);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitInsn(ICONST_2);
            methodVisitor.visitVarInsn(ALOAD, 11);
            methodVisitor.visitInsn(ICONST_2);
            methodVisitor.visitInsn(AALOAD);
            methodVisitor.visitTypeInsn(CHECKCAST, "kotlinx/serialization/DeserializationStrategy");
            methodVisitor.visitVarInsn(ALOAD, 8);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "kotlinx/serialization/encoding/CompositeDecoder", "decodeSerializableElement", "(Lkotlinx/serialization/descriptors/SerialDescriptor;ILkotlinx/serialization/DeserializationStrategy;Ljava/lang/Object;)Ljava/lang/Object;", true);
            methodVisitor.visitTypeInsn(CHECKCAST, "java/util/Map");
            methodVisitor.visitVarInsn(ASTORE, 8);
            methodVisitor.visitVarInsn(ILOAD, 5);
            methodVisitor.visitInsn(ICONST_4);
            methodVisitor.visitInsn(IOR);
            methodVisitor.visitVarInsn(ISTORE, 5);
            methodVisitor.visitVarInsn(ALOAD, 10);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitInsn(ICONST_3);
            methodVisitor.visitFieldInsn(GETSTATIC, "kotlinx/serialization/internal/BooleanSerializer", "INSTANCE", "Lkotlinx/serialization/internal/BooleanSerializer;");
            methodVisitor.visitTypeInsn(CHECKCAST, "kotlinx/serialization/DeserializationStrategy");
            methodVisitor.visitVarInsn(ALOAD, 9);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "kotlinx/serialization/encoding/CompositeDecoder", "decodeNullableSerializableElement", "(Lkotlinx/serialization/descriptors/SerialDescriptor;ILkotlinx/serialization/DeserializationStrategy;Ljava/lang/Object;)Ljava/lang/Object;", true);
            methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
            methodVisitor.visitVarInsn(ASTORE, 9);
            methodVisitor.visitVarInsn(ILOAD, 5);
            methodVisitor.visitIntInsn(BIPUSH, 8);
            methodVisitor.visitInsn(IOR);
            methodVisitor.visitVarInsn(ISTORE, 5);
            Label label1 = new Label();
            methodVisitor.visitJumpInsn(GOTO, label1);
            methodVisitor.visitLabel(label0);
            methodVisitor.visitFrame(Opcodes.F_FULL, 12, new Object[]{"plugin/Version$$serializer", "kotlinx/serialization/encoding/Decoder", "kotlinx/serialization/descriptors/SerialDescriptor", Opcodes.INTEGER, Opcodes.TOP, Opcodes.INTEGER, "java/lang/String", "java/lang/String", "java/util/Map", "java/lang/Boolean", "kotlinx/serialization/encoding/CompositeDecoder", "[Lkotlinx/serialization/KSerializer;"}, 0, new Object[]{});
            methodVisitor.visitVarInsn(ILOAD, 3);
            methodVisitor.visitJumpInsn(IFEQ, label1);
            methodVisitor.visitVarInsn(ALOAD, 10);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "kotlinx/serialization/encoding/CompositeDecoder", "decodeElementIndex", "(Lkotlinx/serialization/descriptors/SerialDescriptor;)I", true);
            methodVisitor.visitVarInsn(ISTORE, 4);
            methodVisitor.visitVarInsn(ILOAD, 4);
            Label label2 = new Label();
            Label label3 = new Label();
            Label label4 = new Label();
            Label label5 = new Label();
            Label label6 = new Label();
            Label label7 = new Label();
            methodVisitor.visitTableSwitchInsn(-1, 3, label7, label2, label3, label4, label5, label6);
            methodVisitor.visitLabel(label2);
            methodVisitor.visitFrame(Opcodes.F_FULL, 12, new Object[]{"plugin/Version$$serializer", "kotlinx/serialization/encoding/Decoder", "kotlinx/serialization/descriptors/SerialDescriptor", Opcodes.INTEGER, Opcodes.INTEGER, Opcodes.INTEGER, "java/lang/String", "java/lang/String", "java/util/Map", "java/lang/Boolean", "kotlinx/serialization/encoding/CompositeDecoder", "[Lkotlinx/serialization/KSerializer;"}, 0, new Object[]{});
            methodVisitor.visitInsn(ICONST_0);
            methodVisitor.visitVarInsn(ISTORE, 3);
            methodVisitor.visitJumpInsn(GOTO, label0);
            methodVisitor.visitLabel(label3);
            methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            methodVisitor.visitVarInsn(ALOAD, 10);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitInsn(ICONST_0);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "kotlinx/serialization/encoding/CompositeDecoder", "decodeStringElement", "(Lkotlinx/serialization/descriptors/SerialDescriptor;I)Ljava/lang/String;", true);
            methodVisitor.visitVarInsn(ASTORE, 6);
            methodVisitor.visitVarInsn(ILOAD, 5);
            methodVisitor.visitInsn(ICONST_1);
            methodVisitor.visitInsn(IOR);
            methodVisitor.visitVarInsn(ISTORE, 5);
            methodVisitor.visitJumpInsn(GOTO, label0);
            methodVisitor.visitLabel(label4);
            methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            methodVisitor.visitVarInsn(ALOAD, 10);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitInsn(ICONST_1);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "kotlinx/serialization/encoding/CompositeDecoder", "decodeStringElement", "(Lkotlinx/serialization/descriptors/SerialDescriptor;I)Ljava/lang/String;", true);
            methodVisitor.visitVarInsn(ASTORE, 7);
            methodVisitor.visitVarInsn(ILOAD, 5);
            methodVisitor.visitInsn(ICONST_2);
            methodVisitor.visitInsn(IOR);
            methodVisitor.visitVarInsn(ISTORE, 5);
            methodVisitor.visitJumpInsn(GOTO, label0);
            methodVisitor.visitLabel(label5);
            methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            methodVisitor.visitVarInsn(ALOAD, 10);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitInsn(ICONST_2);
            methodVisitor.visitVarInsn(ALOAD, 11);
            methodVisitor.visitInsn(ICONST_2);
            methodVisitor.visitInsn(AALOAD);
            methodVisitor.visitTypeInsn(CHECKCAST, "kotlinx/serialization/DeserializationStrategy");
            methodVisitor.visitVarInsn(ALOAD, 8);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "kotlinx/serialization/encoding/CompositeDecoder", "decodeSerializableElement", "(Lkotlinx/serialization/descriptors/SerialDescriptor;ILkotlinx/serialization/DeserializationStrategy;Ljava/lang/Object;)Ljava/lang/Object;", true);
            methodVisitor.visitTypeInsn(CHECKCAST, "java/util/Map");
            methodVisitor.visitVarInsn(ASTORE, 8);
            methodVisitor.visitVarInsn(ILOAD, 5);
            methodVisitor.visitInsn(ICONST_4);
            methodVisitor.visitInsn(IOR);
            methodVisitor.visitVarInsn(ISTORE, 5);
            methodVisitor.visitJumpInsn(GOTO, label0);
            methodVisitor.visitLabel(label6);
            methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            methodVisitor.visitVarInsn(ALOAD, 10);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitInsn(ICONST_3);
            methodVisitor.visitFieldInsn(GETSTATIC, "kotlinx/serialization/internal/BooleanSerializer", "INSTANCE", "Lkotlinx/serialization/internal/BooleanSerializer;");
            methodVisitor.visitTypeInsn(CHECKCAST, "kotlinx/serialization/DeserializationStrategy");
            methodVisitor.visitVarInsn(ALOAD, 9);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "kotlinx/serialization/encoding/CompositeDecoder", "decodeNullableSerializableElement", "(Lkotlinx/serialization/descriptors/SerialDescriptor;ILkotlinx/serialization/DeserializationStrategy;Ljava/lang/Object;)Ljava/lang/Object;", true);
            methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
            methodVisitor.visitVarInsn(ASTORE, 9);
            methodVisitor.visitVarInsn(ILOAD, 5);
            methodVisitor.visitIntInsn(BIPUSH, 8);
            methodVisitor.visitInsn(IOR);
            methodVisitor.visitVarInsn(ISTORE, 5);
            methodVisitor.visitJumpInsn(GOTO, label0);
            methodVisitor.visitLabel(label7);
            methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            methodVisitor.visitTypeInsn(NEW, "kotlinx/serialization/UnknownFieldException");
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitVarInsn(ILOAD, 4);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "kotlinx/serialization/UnknownFieldException", "<init>", "(I)V", false);
            methodVisitor.visitInsn(ATHROW);
            methodVisitor.visitLabel(label1);
            methodVisitor.visitFrame(Opcodes.F_FULL, 12, new Object[]{"plugin/Version$$serializer", "kotlinx/serialization/encoding/Decoder", "kotlinx/serialization/descriptors/SerialDescriptor", Opcodes.INTEGER, Opcodes.TOP, Opcodes.INTEGER, "java/lang/String", "java/lang/String", "java/util/Map", "java/lang/Boolean", "kotlinx/serialization/encoding/CompositeDecoder", "[Lkotlinx/serialization/KSerializer;"}, 0, new Object[]{});
            methodVisitor.visitVarInsn(ALOAD, 10);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "kotlinx/serialization/encoding/CompositeDecoder", "endStructure", "(Lkotlinx/serialization/descriptors/SerialDescriptor;)V", true);
            methodVisitor.visitTypeInsn(NEW, "plugin/Version");
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitVarInsn(ILOAD, 5);
            methodVisitor.visitVarInsn(ALOAD, 6);
            methodVisitor.visitVarInsn(ALOAD, 7);
            methodVisitor.visitVarInsn(ALOAD, 8);
            methodVisitor.visitVarInsn(ALOAD, 9);
            methodVisitor.visitInsn(ACONST_NULL);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "plugin/Version", "<init>", "(ILjava/lang/String;Ljava/lang/String;Ljava/util/Map;Ljava/lang/Boolean;Lkotlinx/serialization/internal/SerializationConstructorMarker;)V", false);
            methodVisitor.visitInsn(ARETURN);
            methodVisitor.visitMaxs(8, 12);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_FINAL, "getDescriptor", "()Lkotlinx/serialization/descriptors/SerialDescriptor;", null, null);
            {
                annotationVisitor0 = methodVisitor.visitAnnotation("Lorg/jetbrains/annotations/NotNull;", false);
                annotationVisitor0.visitEnd();
            }
            methodVisitor.visitCode();
            methodVisitor.visitFieldInsn(GETSTATIC, "plugin/Version$$serializer", "descriptor", "Lkotlinx/serialization/descriptors/SerialDescriptor;");
            methodVisitor.visitInsn(ARETURN);
            methodVisitor.visitMaxs(1, 1);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_FINAL, "childSerializers", "()[Lkotlinx/serialization/KSerializer;", "()[Lkotlinx/serialization/KSerializer<*>;", null);
            {
                annotationVisitor0 = methodVisitor.visitAnnotation("Lorg/jetbrains/annotations/NotNull;", false);
                annotationVisitor0.visitEnd();
            }
            methodVisitor.visitCode();
            methodVisitor.visitMethodInsn(INVOKESTATIC, "plugin/Version", "access$get$childSerializers$cp", "()[Lkotlinx/serialization/KSerializer;", false);
            methodVisitor.visitVarInsn(ASTORE, 1);
            methodVisitor.visitInsn(ICONST_4);
            methodVisitor.visitTypeInsn(ANEWARRAY, "kotlinx/serialization/KSerializer");
            methodVisitor.visitVarInsn(ASTORE, 2);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitInsn(ICONST_0);
            methodVisitor.visitFieldInsn(GETSTATIC, "kotlinx/serialization/internal/StringSerializer", "INSTANCE", "Lkotlinx/serialization/internal/StringSerializer;");
            methodVisitor.visitInsn(AASTORE);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitInsn(ICONST_1);
            methodVisitor.visitFieldInsn(GETSTATIC, "kotlinx/serialization/internal/StringSerializer", "INSTANCE", "Lkotlinx/serialization/internal/StringSerializer;");
            methodVisitor.visitInsn(AASTORE);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitInsn(ICONST_2);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitInsn(ICONST_2);
            methodVisitor.visitInsn(AALOAD);
            methodVisitor.visitInsn(AASTORE);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitInsn(ICONST_3);
            methodVisitor.visitFieldInsn(GETSTATIC, "kotlinx/serialization/internal/BooleanSerializer", "INSTANCE", "Lkotlinx/serialization/internal/BooleanSerializer;");
            methodVisitor.visitTypeInsn(CHECKCAST, "kotlinx/serialization/KSerializer");
            methodVisitor.visitMethodInsn(INVOKESTATIC, "kotlinx/serialization/builtins/BuiltinSerializersKt", "getNullable", "(Lkotlinx/serialization/KSerializer;)Lkotlinx/serialization/KSerializer;", false);
            methodVisitor.visitInsn(AASTORE);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitInsn(ARETURN);
            methodVisitor.visitMaxs(4, 3);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "typeParametersSerializers", "()[Lkotlinx/serialization/KSerializer;", "()[Lkotlinx/serialization/KSerializer<*>;", null);
            {
                annotationVisitor0 = methodVisitor.visitAnnotation("Lorg/jetbrains/annotations/NotNull;", false);
                annotationVisitor0.visitEnd();
            }
            methodVisitor.visitCode();
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "kotlinx/serialization/internal/GeneratedSerializer$DefaultImpls", "typeParametersSerializers", "(Lkotlinx/serialization/internal/GeneratedSerializer;)[Lkotlinx/serialization/KSerializer;", false);
            methodVisitor.visitInsn(ARETURN);
            methodVisitor.visitMaxs(1, 1);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC, "serialize", "(Lkotlinx/serialization/encoding/Encoder;Ljava/lang/Object;)V", null, null);
            methodVisitor.visitCode();
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitTypeInsn(CHECKCAST, "plugin/Version");
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "plugin/Version$$serializer", "serialize", "(Lkotlinx/serialization/encoding/Encoder;Lplugin/Version;)V", false);
            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(3, 3);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC, "deserialize", "(Lkotlinx/serialization/encoding/Decoder;)Ljava/lang/Object;", null, null);
            methodVisitor.visitCode();
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "plugin/Version$$serializer", "deserialize", "(Lkotlinx/serialization/encoding/Decoder;)Lplugin/Version;", false);
            methodVisitor.visitInsn(ARETURN);
            methodVisitor.visitMaxs(2, 2);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            methodVisitor.visitCode();
            methodVisitor.visitTypeInsn(NEW, "plugin/Version$$serializer");
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "plugin/Version$$serializer", "<init>", "()V", false);
            methodVisitor.visitFieldInsn(PUTSTATIC, "plugin/Version$$serializer", "INSTANCE", "Lplugin/Version$$serializer;");
            methodVisitor.visitTypeInsn(NEW, "kotlinx/serialization/internal/PluginGeneratedSerialDescriptor");
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitLdcInsn("com.github.novotnyr.kotlinpluginideaplugin.Version");
            methodVisitor.visitFieldInsn(GETSTATIC, "plugin/Version$$serializer", "INSTANCE", "Lplugin/Version$$serializer;");
            methodVisitor.visitTypeInsn(CHECKCAST, "kotlinx/serialization/internal/GeneratedSerializer");
            methodVisitor.visitInsn(ICONST_4);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "kotlinx/serialization/internal/PluginGeneratedSerialDescriptor", "<init>", "(Ljava/lang/String;Lkotlinx/serialization/internal/GeneratedSerializer;I)V", false);
            methodVisitor.visitVarInsn(ASTORE, 0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitLdcInsn("version");
            methodVisitor.visitInsn(ICONST_1);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "kotlinx/serialization/internal/PluginGeneratedSerialDescriptor", "addElement", "(Ljava/lang/String;Z)V", false);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitLdcInsn("revision");
            methodVisitor.visitInsn(ICONST_1);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "kotlinx/serialization/internal/PluginGeneratedSerialDescriptor", "addElement", "(Ljava/lang/String;Z)V", false);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitLdcInsn("metadata");
            methodVisitor.visitInsn(ICONST_1);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "kotlinx/serialization/internal/PluginGeneratedSerialDescriptor", "addElement", "(Ljava/lang/String;Z)V", false);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitLdcInsn("advanced");
            methodVisitor.visitInsn(ICONST_1);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "kotlinx/serialization/internal/PluginGeneratedSerialDescriptor", "addElement", "(Ljava/lang/String;Z)V", false);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitTypeInsn(CHECKCAST, "kotlinx/serialization/descriptors/SerialDescriptor");
            methodVisitor.visitFieldInsn(PUTSTATIC, "plugin/Version$$serializer", "descriptor", "Lkotlinx/serialization/descriptors/SerialDescriptor;");
            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(5, 1);
            methodVisitor.visitEnd();
        }
        classWriter.visitEnd();

        return classWriter.toByteArray();
    }
}
