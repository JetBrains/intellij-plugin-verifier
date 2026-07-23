/*
 * Class-file dumps for KotlinInlineFunctionExperimentalApiUsageTest, mirroring
 * KotlinInlineFunctionDumps.kt (MP-7133) with `@ApiStatus.Experimental` instead of
 * `@ApiStatus.Internal`, obtained the same way: compiling the Kotlin sources quoted in each
 * KDoc with Kotlin 2.0.21 and converting them with ASMifier.
 */
package com.jetbrains.pluginverifier.tests.bytecode

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*

/**
 * `@ApiStatus.Experimental` SDK interface:
 * ```
 * package com.intellij.platform.util.progress
 *
 * @ApiStatus.Experimental
 * interface ExperimentalReporterHandle {
 *   val reporter: String
 *   fun close()
 * }
 * ```
 */
object ExperimentalReporterHandleDump {
  fun dump(): ByteArray {
    val classWriter = ClassWriter(0)
    classWriter.visit(
      V11,
      ACC_PUBLIC or ACC_ABSTRACT or ACC_INTERFACE,
      "com/intellij/platform/util/progress/ExperimentalReporterHandle",
      null,
      "java/lang/Object",
      null
    )
    classWriter.visitSource("experimentalProgress.kt", null)
    classWriter.visitAnnotation("Lorg/jetbrains/annotations/ApiStatus\$Experimental;", false).visitEnd()
    classWriter.visitInnerClass(
      "org/jetbrains/annotations/ApiStatus\$Experimental",
      "org/jetbrains/annotations/ApiStatus",
      "Experimental",
      ACC_PUBLIC or ACC_STATIC or ACC_ANNOTATION or ACC_ABSTRACT or ACC_INTERFACE
    )
    classWriter.visitMethod(ACC_PUBLIC or ACC_ABSTRACT, "getReporter", "()Ljava/lang/String;", null, null).visitEnd()
    classWriter.visitMethod(ACC_PUBLIC or ACC_ABSTRACT, "close", "()V", null, null).visitEnd()
    classWriter.visitEnd()
    return classWriter.toByteArray()
  }
}

/**
 * SDK file facade holding the `@ApiStatus.Experimental` function:
 * ```
 * @ApiStatus.Experimental
 * fun experimentalCurrentStep(): ExperimentalReporterHandle = ...
 * ```
 * The real facade also declares the public `inline fun reportExperimentalProgress`, which
 * compiled callers never reference: its body is inlined at each call site.
 */
object ExperimentalProgressKtDump {
  fun dump(): ByteArray {
    val classWriter = ClassWriter(0)
    classWriter.visit(
      V11,
      ACC_PUBLIC or ACC_FINAL or ACC_SUPER,
      "com/intellij/platform/util/progress/ExperimentalProgressKt",
      null,
      "java/lang/Object",
      null
    )
    classWriter.visitSource("experimentalProgress.kt", null)
    classWriter.visitInnerClass(
      "org/jetbrains/annotations/ApiStatus\$Experimental",
      "org/jetbrains/annotations/ApiStatus",
      "Experimental",
      ACC_PUBLIC or ACC_STATIC or ACC_ANNOTATION or ACC_ABSTRACT or ACC_INTERFACE
    )
    classWriter.visitMethod(
      ACC_PUBLIC or ACC_FINAL or ACC_STATIC,
      "experimentalCurrentStep",
      "()Lcom/intellij/platform/util/progress/ExperimentalReporterHandle;",
      null,
      null
    ).apply {
      visitAnnotation("Lorg/jetbrains/annotations/ApiStatus\$Experimental;", false).visitEnd()
      visitCode()
      visitInsn(ACONST_NULL)
      visitInsn(ARETURN)
      visitMaxs(1, 0)
      visitEnd()
    }
    classWriter.visitEnd()
    return classWriter.toByteArray()
  }
}

/**
 * The plugin class:
 * ```
 * package com.example.plugin
 *
 * import com.intellij.platform.util.progress.reportExperimentalProgress
 *
 * class MyExperimentalAction {
 *   fun run(): Int = reportExperimentalProgress { reporter -> reporter.length }
 * }
 * ```
 * The source only calls the public `reportExperimentalProgress`, but the compiled `run()`
 * contains the inlined experimental API calls plus the compiler's inlining markers, and an SMAP
 * mapping output lines 10..14 to `experimentalProgress.kt`.
 */
object ExperimentalInlinedCallerDump {
  private const val SMAP = "SMAP\nMyExperimentalAction.kt\nKotlin\n*S Kotlin\n*F\n+ 1 MyExperimentalAction.kt\n" +
    "com/example/plugin/MyExperimentalAction\n+ 2 experimentalProgress.kt\n" +
    "com/intellij/platform/util/progress/ExperimentalProgressKt\n*L\n1#1,9:1\n23#2,5:10\n" +
    "*S KotlinDebug\n*F\n+ 1 MyExperimentalAction.kt\ncom/example/plugin/MyExperimentalAction\n*L\n7#1:10,5\n*E\n"

  fun dump(): ByteArray {
    val classWriter = ClassWriter(0)

    classWriter.visit(V11, ACC_PUBLIC or ACC_FINAL or ACC_SUPER, "com/example/plugin/MyExperimentalAction", null, "java/lang/Object", null)
    classWriter.visitSource("MyExperimentalAction.kt", SMAP)

    classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null).apply {
      visitCode()
      visitVarInsn(ALOAD, 0)
      visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
      visitInsn(RETURN)
      visitMaxs(1, 1)
      visitEnd()
    }

    classWriter.visitMethod(ACC_PUBLIC or ACC_FINAL, "run", "()I", null, null).apply {
      visitCode()
      val label0 = Label()
      val label1 = Label()
      val label2 = Label()
      visitTryCatchBlock(label0, label1, label2, null)
      val label3 = Label()
      visitTryCatchBlock(label2, label3, label2, null)
      val label4 = Label()
      visitLabel(label4)
      visitLineNumber(7, label4)
      visitInsn(ICONST_0)
      visitVarInsn(ISTORE, 1)
      val label5 = Label()
      visitLabel(label5)
      visitLineNumber(10, label5)
      visitMethodInsn(
        INVOKESTATIC,
        "com/intellij/platform/util/progress/ExperimentalProgressKt",
        "experimentalCurrentStep",
        "()Lcom/intellij/platform/util/progress/ExperimentalReporterHandle;",
        false
      )
      visitVarInsn(ASTORE, 2)
      visitLabel(label0)
      visitLineNumber(11, label0)
      visitInsn(NOP)
      val label6 = Label()
      visitLabel(label6)
      visitLineNumber(12, label6)
      visitVarInsn(ALOAD, 2)
      visitMethodInsn(
        INVOKEINTERFACE,
        "com/intellij/platform/util/progress/ExperimentalReporterHandle",
        "getReporter",
        "()Ljava/lang/String;",
        true
      )
      visitVarInsn(ASTORE, 3)
      val label7 = Label()
      visitLabel(label7)
      visitInsn(ICONST_0)
      visitVarInsn(ISTORE, 4)
      val label8 = Label()
      visitLabel(label8)
      visitLineNumber(7, label8)
      visitVarInsn(ALOAD, 3)
      visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "length", "()I", false)
      val label9 = Label()
      visitLabel(label9)
      visitLineNumber(12, label9)
      visitVarInsn(ISTORE, 3)
      visitLabel(label1)
      visitLineNumber(14, label1)
      visitVarInsn(ALOAD, 2)
      visitMethodInsn(
        INVOKEINTERFACE,
        "com/intellij/platform/util/progress/ExperimentalReporterHandle",
        "close",
        "()V",
        true
      )
      visitVarInsn(ILOAD, 3)
      val label10 = Label()
      visitLabel(label10)
      visitLineNumber(12, label10)
      val label11 = Label()
      visitJumpInsn(GOTO, label11)
      visitLabel(label2)
      visitLineNumber(14, label2)
      visitFrame(
        F_FULL,
        3,
        arrayOf<Any>("com/example/plugin/MyExperimentalAction", INTEGER, "com/intellij/platform/util/progress/ExperimentalReporterHandle"),
        1,
        arrayOf<Any>("java/lang/Throwable")
      )
      visitVarInsn(ASTORE, 4)
      visitLabel(label3)
      visitVarInsn(ALOAD, 2)
      visitMethodInsn(
        INVOKEINTERFACE,
        "com/intellij/platform/util/progress/ExperimentalReporterHandle",
        "close",
        "()V",
        true
      )
      visitVarInsn(ALOAD, 4)
      visitInsn(ATHROW)
      visitLabel(label11)
      visitLineNumber(7, label11)
      visitFrame(
        F_FULL,
        5,
        arrayOf<Any>("com/example/plugin/MyExperimentalAction", INTEGER, "com/intellij/platform/util/progress/ExperimentalReporterHandle", INTEGER, INTEGER),
        1,
        arrayOf<Any>(INTEGER)
      )
      visitInsn(IRETURN)
      val label12 = Label()
      visitLabel(label12)
      visitLocalVariable("\$i\$a\$-reportExperimentalProgress-MyExperimentalAction\$run\$1", "I", null, label8, label9, 4)
      visitLocalVariable("reporter", "Ljava/lang/String;", null, label7, label9, 3)
      visitLocalVariable("\$i\$f\$reportExperimentalProgress", "I", null, label5, label11, 1)
      visitLocalVariable("handle\$iv", "Lcom/intellij/platform/util/progress/ExperimentalReporterHandle;", null, label0, label11, 2)
      visitLocalVariable("this", "Lcom/example/plugin/MyExperimentalAction;", null, label4, label12, 0)
      visitMaxs(1, 5)
      visitEnd()
    }

    classWriter.visitEnd()
    return classWriter.toByteArray()
  }
}

/**
 * Control case: plugin code invoking the experimental API directly, without any inlining
 * markers:
 * ```
 * package com.example.plugin
 *
 * class DirectExperimentalUsage {
 *   fun run(): Int = experimentalCurrentStep().reporter.length
 * }
 * ```
 * Such usage must always be reported.
 */
object DirectExperimentalCallerDump {
  fun dump(): ByteArray {
    val classWriter = ClassWriter(0)

    classWriter.visit(V11, ACC_PUBLIC or ACC_FINAL or ACC_SUPER, "com/example/plugin/DirectExperimentalUsage", null, "java/lang/Object", null)
    classWriter.visitSource("DirectExperimentalUsage.kt", null)

    classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null).apply {
      visitCode()
      visitVarInsn(ALOAD, 0)
      visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
      visitInsn(RETURN)
      visitMaxs(1, 1)
      visitEnd()
    }

    classWriter.visitMethod(ACC_PUBLIC or ACC_FINAL, "run", "()I", null, null).apply {
      visitCode()
      val label0 = Label()
      visitLabel(label0)
      visitMethodInsn(
        INVOKESTATIC,
        "com/intellij/platform/util/progress/ExperimentalProgressKt",
        "experimentalCurrentStep",
        "()Lcom/intellij/platform/util/progress/ExperimentalReporterHandle;",
        false
      )
      visitVarInsn(ASTORE, 1)
      visitVarInsn(ALOAD, 1)
      visitMethodInsn(
        INVOKEINTERFACE,
        "com/intellij/platform/util/progress/ExperimentalReporterHandle",
        "getReporter",
        "()Ljava/lang/String;",
        true
      )
      visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "length", "()I", false)
      visitInsn(IRETURN)
      val label1 = Label()
      visitLabel(label1)
      visitLocalVariable("this", "Lcom/example/plugin/DirectExperimentalUsage;", null, label0, label1, 0)
      visitLocalVariable("handle", "Lcom/intellij/platform/util/progress/ExperimentalReporterHandle;", null, label0, label1, 1)
      visitMaxs(1, 2)
      visitEnd()
    }

    classWriter.visitEnd()
    return classWriter.toByteArray()
  }
}

/**
 * Caller whose inlined code originates from the plugin's own
 * `inline fun useExperimentalProgress() { experimentalCurrentStep() }` declared in its
 * `utils.kt`:
 * ```
 * package com.example.plugin
 *
 * class OwnExperimentalCaller {
 *   fun run(): Int {
 *     useExperimentalProgress()
 *     return 0
 *   }
 * }
 * ```
 * The SMAP maps the inlined lines 10..14 to the plugin's own `UtilsKt` class, so the
 * experimental API usage is the plugin author's code and must still be reported.
 */
object OwnExperimentalInlineCallerDump {
  private const val SMAP = "SMAP\nOwnExperimentalCaller.kt\nKotlin\n*S Kotlin\n*F\n+ 1 OwnExperimentalCaller.kt\n" +
    "com/example/plugin/OwnExperimentalCaller\n+ 2 utils.kt\ncom/example/plugin/ExperimentalUtilsKt\n*L\n1#1,9:1\n23#2,5:10\n" +
    "*S KotlinDebug\n*F\n+ 1 OwnExperimentalCaller.kt\ncom/example/plugin/OwnExperimentalCaller\n*L\n7#1:10,5\n*E\n"

  fun dump(): ByteArray {
    val classWriter = ClassWriter(0)
    classWriter.visit(V11, ACC_PUBLIC or ACC_FINAL or ACC_SUPER, "com/example/plugin/OwnExperimentalCaller", null, "java/lang/Object", null)
    classWriter.visitSource("OwnExperimentalCaller.kt", SMAP)

    classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null).apply {
      visitCode()
      visitVarInsn(ALOAD, 0)
      visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
      visitInsn(RETURN)
      visitMaxs(1, 1)
      visitEnd()
    }

    classWriter.visitMethod(ACC_PUBLIC or ACC_FINAL, "run", "()I", null, null).apply {
      visitCode()
      val label0 = Label()
      visitLabel(label0)
      visitLineNumber(7, label0)
      visitInsn(ICONST_0)
      visitVarInsn(ISTORE, 1)
      val label1 = Label()
      visitLabel(label1)
      visitLineNumber(10, label1)
      visitMethodInsn(
        INVOKESTATIC,
        "com/intellij/platform/util/progress/ExperimentalProgressKt",
        "experimentalCurrentStep",
        "()Lcom/intellij/platform/util/progress/ExperimentalReporterHandle;",
        false
      )
      visitInsn(POP)
      val label2 = Label()
      visitLabel(label2)
      visitLineNumber(8, label2)
      visitInsn(ICONST_0)
      visitInsn(IRETURN)
      val label3 = Label()
      visitLabel(label3)
      visitLocalVariable("\$i\$f\$useExperimentalProgress", "I", null, label1, label2, 1)
      visitLocalVariable("this", "Lcom/example/plugin/OwnExperimentalCaller;", null, label0, label3, 0)
      visitMaxs(1, 2)
      visitEnd()
    }

    classWriter.visitEnd()
    return classWriter.toByteArray()
  }
}

/**
 * The plugin's own `utils.kt` file facade declaring `inline fun useExperimentalProgress()`. Its
 * presence lets the verifier resolve the SMAP origin of [OwnExperimentalInlineCallerDump] to a
 * class of the plugin.
 */
object PluginExperimentalUtilsKtDump {
  fun dump(): ByteArray {
    val classWriter = ClassWriter(0)
    classWriter.visit(V11, ACC_PUBLIC or ACC_FINAL or ACC_SUPER, "com/example/plugin/ExperimentalUtilsKt", null, "java/lang/Object", null)
    classWriter.visitSource("utils.kt", null)
    classWriter.visitEnd()
    return classWriter.toByteArray()
  }
}
