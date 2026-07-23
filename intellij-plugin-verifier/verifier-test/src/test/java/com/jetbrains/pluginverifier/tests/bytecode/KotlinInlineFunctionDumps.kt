/*
 * Class-file dumps for KotlinInlineFunctionInternalApiUsageTest (MP-7133), obtained by compiling
 * the Kotlin sources quoted in each KDoc with Kotlin 2.0.21 and converting them with ASMifier.
 */
package com.jetbrains.pluginverifier.tests.bytecode

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*

/**
 * `@ApiStatus.Internal` SDK interface:
 * ```
 * package com.intellij.platform.util.progress
 *
 * @ApiStatus.Internal
 * interface RawProgressReporterHandle {
 *   val reporter: String
 *   fun close()
 * }
 * ```
 */
object RawProgressReporterHandleDump {
  fun dump(): ByteArray {
    val classWriter = ClassWriter(0)
    classWriter.visit(
      V11,
      ACC_PUBLIC or ACC_ABSTRACT or ACC_INTERFACE,
      "com/intellij/platform/util/progress/RawProgressReporterHandle",
      null,
      "java/lang/Object",
      null
    )
    classWriter.visitSource("rawProgress.kt", null)
    classWriter.visitAnnotation("Lorg/jetbrains/annotations/ApiStatus\$Internal;", false).visitEnd()
    classWriter.visitInnerClass(
      "org/jetbrains/annotations/ApiStatus\$Internal",
      "org/jetbrains/annotations/ApiStatus",
      "Internal",
      ACC_PUBLIC or ACC_STATIC or ACC_ANNOTATION or ACC_ABSTRACT or ACC_INTERFACE
    )
    classWriter.visitMethod(ACC_PUBLIC or ACC_ABSTRACT, "getReporter", "()Ljava/lang/String;", null, null).visitEnd()
    classWriter.visitMethod(ACC_PUBLIC or ACC_ABSTRACT, "close", "()V", null, null).visitEnd()
    classWriter.visitEnd()
    return classWriter.toByteArray()
  }
}

/**
 * SDK file facade holding the `@ApiStatus.Internal` function:
 * ```
 * @ApiStatus.Internal
 * fun internalCurrentStepAsRaw(): RawProgressReporterHandle = ...
 * ```
 * The real facade also declares the public `inline fun reportRawProgress`, which compiled
 * callers never reference: its body is inlined at each call site.
 */
object RawProgressKtDump {
  fun dump(): ByteArray {
    val classWriter = ClassWriter(0)
    classWriter.visit(
      V11,
      ACC_PUBLIC or ACC_FINAL or ACC_SUPER,
      "com/intellij/platform/util/progress/RawProgressKt",
      null,
      "java/lang/Object",
      null
    )
    classWriter.visitSource("rawProgress.kt", null)
    classWriter.visitInnerClass(
      "org/jetbrains/annotations/ApiStatus\$Internal",
      "org/jetbrains/annotations/ApiStatus",
      "Internal",
      ACC_PUBLIC or ACC_STATIC or ACC_ANNOTATION or ACC_ABSTRACT or ACC_INTERFACE
    )
    classWriter.visitMethod(
      ACC_PUBLIC or ACC_FINAL or ACC_STATIC,
      "internalCurrentStepAsRaw",
      "()Lcom/intellij/platform/util/progress/RawProgressReporterHandle;",
      null,
      null
    ).apply {
      visitAnnotation("Lorg/jetbrains/annotations/ApiStatus\$Internal;", false).visitEnd()
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
 * import com.intellij.platform.util.progress.reportRawProgress
 *
 * class MyAction {
 *   fun run(): Int = reportRawProgress { reporter -> reporter.length }
 * }
 * ```
 * The source only calls the public `reportRawProgress`, but the compiled `run()` contains the
 * inlined internal API calls plus the compiler's inlining markers: the `$i$f$`/`$i$a$`/`$iv`
 * fake locals and the SMAP mapping output lines 10..14 to `rawProgress.kt`, emitted both as the
 * `SourceDebugExtension` attribute and as the `kotlin.jvm.internal.SourceDebugExtension` annotation.
 * Pass `smapInSourceAttribute = false` to emulate a tool that stripped the attribute.
 */
object InlinedCallerDump {
  private const val SMAP = "SMAP\nMyAction.kt\nKotlin\n*S Kotlin\n*F\n+ 1 MyAction.kt\ncom/example/plugin/MyAction\n" +
    "+ 2 rawProgress.kt\ncom/intellij/platform/util/progress/RawProgressKt\n*L\n1#1,9:1\n23#2,5:10\n" +
    "*S KotlinDebug\n*F\n+ 1 MyAction.kt\ncom/example/plugin/MyAction\n*L\n7#1:10,5\n*E\n"

  fun dump(smapInSourceAttribute: Boolean = true): ByteArray {
    val classWriter = ClassWriter(0)

    classWriter.visit(V11, ACC_PUBLIC or ACC_FINAL or ACC_SUPER, "com/example/plugin/MyAction", null, "java/lang/Object", null)
    classWriter.visitSource("MyAction.kt", if (smapInSourceAttribute) SMAP else null)

    classWriter.visitAnnotation("Lkotlin/Metadata;", true).apply {
      visit("mv", intArrayOf(2, 0, 0))
      visit("k", 1)
      visit("xi", 48)
      visitArray("d1").apply {
        visit(
          null,
          "\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\u0008\u0003\n\u0002\u0010\u0008\n\u0000\u0018" +
            "\u00002\u00020\u0001B\u0007\u00a2\u0006\u0004\u0008\u0002\u0010\u0003J\u0006\u0010\u0004\u001a\u00020\u0005\u00a8\u0006\u0006"
        )
        visitEnd()
      }
      visitArray("d2").apply {
        visit(null, "Lcom/example/plugin/MyAction;")
        visit(null, "")
        visit(null, "<init>")
        visit(null, "()V")
        visit(null, "run")
        visit(null, "")
        visit(null, "plugin")
        visitEnd()
      }
      visitEnd()
    }

    classWriter.visitAnnotation("Lkotlin/jvm/internal/SourceDebugExtension;", false).apply {
      visitArray("value").apply {
        visit(null, SMAP)
        visitEnd()
      }
      visitEnd()
    }

    classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null).apply {
      visitCode()
      val label0 = Label()
      visitLabel(label0)
      visitLineNumber(6, label0)
      visitVarInsn(ALOAD, 0)
      visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
      visitInsn(RETURN)
      val label1 = Label()
      visitLabel(label1)
      visitLocalVariable("this", "Lcom/example/plugin/MyAction;", null, label0, label1, 0)
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
        "com/intellij/platform/util/progress/RawProgressKt",
        "internalCurrentStepAsRaw",
        "()Lcom/intellij/platform/util/progress/RawProgressReporterHandle;",
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
        "com/intellij/platform/util/progress/RawProgressReporterHandle",
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
        "com/intellij/platform/util/progress/RawProgressReporterHandle",
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
        arrayOf<Any>("com/example/plugin/MyAction", INTEGER, "com/intellij/platform/util/progress/RawProgressReporterHandle"),
        1,
        arrayOf<Any>("java/lang/Throwable")
      )
      visitVarInsn(ASTORE, 4)
      visitLabel(label3)
      visitVarInsn(ALOAD, 2)
      visitMethodInsn(
        INVOKEINTERFACE,
        "com/intellij/platform/util/progress/RawProgressReporterHandle",
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
        arrayOf<Any>("com/example/plugin/MyAction", INTEGER, "com/intellij/platform/util/progress/RawProgressReporterHandle", INTEGER, INTEGER),
        1,
        arrayOf<Any>(INTEGER)
      )
      visitInsn(IRETURN)
      val label12 = Label()
      visitLabel(label12)
      visitLocalVariable("\$i\$a\$-reportRawProgress-MyAction\$run\$1", "I", null, label8, label9, 4)
      visitLocalVariable("reporter", "Ljava/lang/String;", null, label7, label9, 3)
      visitLocalVariable("\$i\$f\$reportRawProgress", "I", null, label5, label11, 1)
      visitLocalVariable("handle\$iv", "Lcom/intellij/platform/util/progress/RawProgressReporterHandle;", null, label0, label11, 2)
      visitLocalVariable("this", "Lcom/example/plugin/MyAction;", null, label4, label12, 0)
      visitMaxs(1, 5)
      visitEnd()
    }

    classWriter.visitEnd()
    return classWriter.toByteArray()
  }
}

/**
 * Control case: plugin code invoking the internal API directly, without any inlining markers:
 * ```
 * package com.example.plugin
 *
 * class DirectUsage {
 *   fun run(): Int = internalCurrentStepAsRaw().reporter.length
 * }
 * ```
 * Such usage must always be reported.
 */
object DirectCallerDump {
  fun dump(): ByteArray {
    val classWriter = ClassWriter(0)

    classWriter.visit(V11, ACC_PUBLIC or ACC_FINAL or ACC_SUPER, "com/example/plugin/DirectUsage", null, "java/lang/Object", null)
    classWriter.visitSource("DirectUsage.kt", null)

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
        "com/intellij/platform/util/progress/RawProgressKt",
        "internalCurrentStepAsRaw",
        "()Lcom/intellij/platform/util/progress/RawProgressReporterHandle;",
        false
      )
      visitVarInsn(ASTORE, 1)
      visitVarInsn(ALOAD, 1)
      visitMethodInsn(
        INVOKEINTERFACE,
        "com/intellij/platform/util/progress/RawProgressReporterHandle",
        "getReporter",
        "()Ljava/lang/String;",
        true
      )
      visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "length", "()I", false)
      visitInsn(IRETURN)
      val label1 = Label()
      visitLabel(label1)
      visitLocalVariable("this", "Lcom/example/plugin/DirectUsage;", null, label0, label1, 0)
      visitLocalVariable("handle", "Lcom/intellij/platform/util/progress/RawProgressReporterHandle;", null, label0, label1, 1)
      visitMaxs(1, 2)
      visitEnd()
    }

    classWriter.visitEnd()
    return classWriter.toByteArray()
  }
}

/**
 * Caller whose inlined code originates from the plugin's own
 * `inline fun useProgress() { internalCurrentStepAsRaw() }` declared in its `utils.kt`:
 * ```
 * package com.example.plugin
 *
 * class OwnCaller {
 *   fun run(): Int {
 *     useProgress()
 *     return 0
 *   }
 * }
 * ```
 * The SMAP maps the inlined lines 10..14 to the plugin's own `UtilsKt` class, so the internal
 * API usage is the plugin author's code and must still be reported.
 */
object OwnInlineCallerDump {
  private const val SMAP = "SMAP\nOwnCaller.kt\nKotlin\n*S Kotlin\n*F\n+ 1 OwnCaller.kt\ncom/example/plugin/OwnCaller\n+ 2 utils.kt\ncom/example/plugin/UtilsKt\n*L\n1#1,9:1\n23#2,5:10\n*S KotlinDebug\n*F\n+ 1 OwnCaller.kt\ncom/example/plugin/OwnCaller\n*L\n7#1:10,5\n*E\n"

  fun dump(): ByteArray {
    val classWriter = ClassWriter(0)
    classWriter.visit(V11, ACC_PUBLIC or ACC_FINAL or ACC_SUPER, "com/example/plugin/OwnCaller", null, "java/lang/Object", null)
    classWriter.visitSource("OwnCaller.kt", SMAP)

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
        "com/intellij/platform/util/progress/RawProgressKt",
        "internalCurrentStepAsRaw",
        "()Lcom/intellij/platform/util/progress/RawProgressReporterHandle;",
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
      visitLocalVariable("\$i\$f\$useProgress", "I", null, label1, label2, 1)
      visitLocalVariable("this", "Lcom/example/plugin/OwnCaller;", null, label0, label3, 0)
      visitMaxs(1, 2)
      visitEnd()
    }

    classWriter.visitEnd()
    return classWriter.toByteArray()
  }
}

/**
 * The plugin's own `utils.kt` file facade declaring `inline fun useProgress()`. Its presence
 * lets the verifier resolve the SMAP origin of [OwnInlineCallerDump] to a class of the plugin.
 */
object PluginUtilsKtDump {
  fun dump(): ByteArray {
    val classWriter = ClassWriter(0)
    classWriter.visit(V11, ACC_PUBLIC or ACC_FINAL or ACC_SUPER, "com/example/plugin/UtilsKt", null, "java/lang/Object", null)
    classWriter.visitSource("utils.kt", null)
    classWriter.visitEnd()
    return classWriter.toByteArray()
  }
}

/**
 * The `MyAction\$run\$1` suspend lambda class generated for the shape reported in MP-7133:
 * ```
 * class MyAction {
 *   suspend fun run(): Int = withBackgroundProgress(project, title) {
 *     reportRawProgress { reporter -> reporter.length }
 *   }
 * }
 * ```
 * The inlined internal API calls land in the lambda's `invokeSuspend` method, and the SMAP is
 * attached to the lambda class itself. The coroutine machinery is simplified (the class extends
 * `Object` instead of `SuspendLambda`): only the caller shape matters to the detection.
 */
object SuspendLambdaCallerDump {
  private const val SMAP = "SMAP\nMyAction.kt\nKotlin\n*S Kotlin\n*F\n+ 1 MyAction.kt\ncom/example/plugin/MyAction\$run\$1\n+ 2 rawProgress.kt\ncom/intellij/platform/util/progress/RawProgressKt\n*L\n1#1,9:1\n23#2,5:10\n*S KotlinDebug\n*F\n+ 1 MyAction.kt\ncom/example/plugin/MyAction\$run\$1\n*L\n7#1:10,5\n*E\n"

  fun dump(): ByteArray {
    val classWriter = ClassWriter(0)
    classWriter.visit(
      V11,
      ACC_FINAL or ACC_SUPER,
      "com/example/plugin/MyAction\$run\$1",
      null,
      "java/lang/Object",
      null
    )
    classWriter.visitSource("MyAction.kt", SMAP)

    classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null).apply {
      visitCode()
      visitVarInsn(ALOAD, 0)
      visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
      visitInsn(RETURN)
      visitMaxs(1, 1)
      visitEnd()
    }

    classWriter.visitMethod(ACC_PUBLIC or ACC_FINAL, "invokeSuspend", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null).apply {
      visitCode()
      val label0 = Label()
      visitLabel(label0)
      visitLineNumber(7, label0)
      visitInsn(ICONST_0)
      visitVarInsn(ISTORE, 2)
      val label1 = Label()
      visitLabel(label1)
      visitLineNumber(10, label1)
      visitMethodInsn(
        INVOKESTATIC,
        "com/intellij/platform/util/progress/RawProgressKt",
        "internalCurrentStepAsRaw",
        "()Lcom/intellij/platform/util/progress/RawProgressReporterHandle;",
        false
      )
      visitVarInsn(ASTORE, 3)
      val label2 = Label()
      visitLabel(label2)
      visitLineNumber(14, label2)
      visitVarInsn(ALOAD, 3)
      visitMethodInsn(
        INVOKEINTERFACE,
        "com/intellij/platform/util/progress/RawProgressReporterHandle",
        "close",
        "()V",
        true
      )
      val label3 = Label()
      visitLabel(label3)
      visitLineNumber(8, label3)
      visitInsn(ACONST_NULL)
      visitInsn(ARETURN)
      val label4 = Label()
      visitLabel(label4)
      visitLocalVariable("\$i\$f\$reportRawProgress", "I", null, label1, label3, 2)
      visitLocalVariable("handle\$iv", "Lcom/intellij/platform/util/progress/RawProgressReporterHandle;", null, label2, label3, 3)
      visitLocalVariable("this", "Lcom/example/plugin/MyAction\$run\$1;", null, label0, label4, 0)
      visitLocalVariable("result", "Ljava/lang/Object;", null, label0, label4, 1)
      visitMaxs(1, 4)
      visitEnd()
    }

    classWriter.visitEnd()
    return classWriter.toByteArray()
  }
}
