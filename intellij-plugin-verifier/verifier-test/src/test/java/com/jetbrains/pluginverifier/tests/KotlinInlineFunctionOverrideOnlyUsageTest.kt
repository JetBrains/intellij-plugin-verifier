package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.tests.mocks.IdeaPluginSpec
import org.junit.Assert.assertTrue
import org.junit.Test
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*

/**
 * Companion to [KotlinInlineFunctionInternalApiUsageTest] for the `@ApiStatus.OverrideOnly` category:
 * [MP-6705](https://youtrack.jetbrains.com/issue/MP-6705) relaxed `ToolWindowManager.registerToolWindow(task)`
 * from `@ApiStatus.Internal` to `@ApiStatus.OverrideOnly` while keeping the public builder overload
 * `inline`, which merely moved the false positive to the override-only category.
 */
class KotlinInlineFunctionOverrideOnlyUsageTest : BaseBytecodeTest() {

  private val pluginSpec = IdeaPluginSpec("com.example.toolwindow", "Third Party Inc.")

  @Test
  fun `OverrideOnly method invoked from the inlined body of a public SDK inline function is not reported`() {
    val idePlugin = buildIdePlugin(pluginSpec) {
      dirs("com/example/plugin") {
        file("TwCaller.class", InlinedToolWindowCallerDump.dump())
      }
    }

    val verificationResult = runVerification(idePlugin)

    assertTrue(
      "Override-only usage inlined from the public SDK inline builder must not be attributed " +
        "to the plugin. Actual: ${verificationResult.overrideOnlyMethodUsages}",
      verificationResult.overrideOnlyMethodUsages.none { "registerToolWindow" in it.fullDescription }
    )
  }

  @Test
  fun `OverrideOnly method invoked directly by plugin code is reported`() {
    val idePlugin = buildIdePlugin(pluginSpec) {
      dirs("com/example/plugin") {
        file("DirectTwCaller.class", DirectToolWindowCallerDump.dump())
      }
    }

    val verificationResult = runVerification(idePlugin)

    assertTrue(
      "Direct invocation of an @OverrideOnly method must still be reported",
      verificationResult.overrideOnlyMethodUsages.any { "registerToolWindow" in it.fullDescription }
    )
  }

  private fun runVerification(idePlugin: IdePlugin): PluginVerificationResult.Verified {
    val ide = buildIdeWithBundledPlugins(includeKotlinStdLib = true) {
      dirs("com/intellij/openapi/wm") {
        file("ToolWindowManager.class", ToolWindowManagerDump.dump())
        file("ToolWindow.class", ToolWindowDump.dump())
        file("RegisterToolWindowTask.class", RegisterToolWindowTaskDump.dump())
      }
    }
    return VerificationRunner().runPluginVerification(ide, idePlugin) as PluginVerificationResult.Verified
  }

  /**
   * Mimics the post-MP-6705 `com.intellij.openapi.wm.ToolWindowManager`:
   * ```
   * abstract class ToolWindowManager {
   *   @ApiStatus.OverrideOnly
   *   abstract fun registerToolWindow(task: RegisterToolWindowTask): ToolWindow
   * }
   * ```
   */
  private object ToolWindowManagerDump {
    fun dump(): ByteArray {
      val classWriter = ClassWriter(0)
      classWriter.visit(
        V11,
        ACC_PUBLIC or ACC_SUPER or ACC_ABSTRACT,
        "com/intellij/openapi/wm/ToolWindowManager",
        null,
        "java/lang/Object",
        null
      )
      classWriter.visitSource("ToolWindowManager.kt", null)
      classWriter.visitInnerClass(
        "org/jetbrains/annotations/ApiStatus\$OverrideOnly",
        "org/jetbrains/annotations/ApiStatus",
        "OverrideOnly",
        ACC_PUBLIC or ACC_STATIC or ACC_ANNOTATION or ACC_ABSTRACT or ACC_INTERFACE
      )
      classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null).apply {
        visitCode()
        visitVarInsn(ALOAD, 0)
        visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
        visitInsn(RETURN)
        visitMaxs(1, 1)
        visitEnd()
      }
      classWriter.visitMethod(
        ACC_PUBLIC or ACC_ABSTRACT,
        "registerToolWindow",
        "(Lcom/intellij/openapi/wm/RegisterToolWindowTask;)Lcom/intellij/openapi/wm/ToolWindow;",
        null,
        null
      ).apply {
        visitAnnotation("Lorg/jetbrains/annotations/ApiStatus\$OverrideOnly;", false).visitEnd()
        visitEnd()
      }
      classWriter.visitEnd()
      return classWriter.toByteArray()
    }
  }

  private object ToolWindowDump {
    fun dump(): ByteArray {
      val classWriter = ClassWriter(0)
      classWriter.visit(
        V11,
        ACC_PUBLIC or ACC_ABSTRACT or ACC_INTERFACE,
        "com/intellij/openapi/wm/ToolWindow",
        null,
        "java/lang/Object",
        null
      )
      classWriter.visitSource("ToolWindow.kt", null)
      classWriter.visitEnd()
      return classWriter.toByteArray()
    }
  }

  private object RegisterToolWindowTaskDump {
    fun dump(): ByteArray {
      val classWriter = ClassWriter(0)
      classWriter.visit(
        V11,
        ACC_PUBLIC or ACC_FINAL or ACC_SUPER,
        "com/intellij/openapi/wm/RegisterToolWindowTask",
        null,
        "java/lang/Object",
        null
      )
      classWriter.visitSource("RegisterToolWindowTask.kt", null)
      classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null).apply {
        visitCode()
        visitVarInsn(ALOAD, 0)
        visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
        visitInsn(RETURN)
        visitMaxs(1, 1)
        visitEnd()
      }
      classWriter.visitEnd()
      return classWriter.toByteArray()
    }
  }

  /**
   * Plugin bytecode as the Kotlin compiler would emit it for:
   * ```
   * class TwCaller {
   *   fun register(twm: ToolWindowManager): ToolWindow =
   *     twm.registerToolWindow("id") { }   // public inline builder overload
   * }
   * ```
   * The inlined body carries the `$i$f$registerToolWindow` fake-local marker and the SMAP mapping
   * output lines 9..13 to `ToolWindowManager`, and calls the `@OverrideOnly` single-argument
   * overload directly.
   */
  private object InlinedToolWindowCallerDump {
    private const val SMAP = "SMAP\nTwCaller.kt\nKotlin\n*S Kotlin\n*F\n+ 1 TwCaller.kt\ncom/example/plugin/TwCaller\n+ 2 ToolWindowManager.kt\ncom/intellij/openapi/wm/ToolWindowManager\n*L\n1#1,8:1\n88#2,5:9\n*S KotlinDebug\n*F\n+ 1 TwCaller.kt\ncom/example/plugin/TwCaller\n*L\n5#1:9,5\n*E\n"

    fun dump(): ByteArray {
      val classWriter = ClassWriter(0)
      classWriter.visit(
        V11,
        ACC_PUBLIC or ACC_FINAL or ACC_SUPER,
        "com/example/plugin/TwCaller",
        null,
        "java/lang/Object",
        null
      )
      classWriter.visitSource("TwCaller.kt", SMAP)
      classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null).apply {
        visitCode()
        visitVarInsn(ALOAD, 0)
        visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
        visitInsn(RETURN)
        visitMaxs(1, 1)
        visitEnd()
      }
      classWriter.visitMethod(
        ACC_PUBLIC or ACC_FINAL,
        "register",
        "(Lcom/intellij/openapi/wm/ToolWindowManager;)Lcom/intellij/openapi/wm/ToolWindow;",
        null,
        null
      ).apply {
        visitCode()
        val label0 = Label()
        visitLabel(label0)
        visitLineNumber(5, label0)
        visitInsn(ICONST_0)
        visitVarInsn(ISTORE, 2)
        val label1 = Label()
        visitLabel(label1)
        visitLineNumber(10, label1)
        visitVarInsn(ALOAD, 1)
        visitTypeInsn(NEW, "com/intellij/openapi/wm/RegisterToolWindowTask")
        visitInsn(DUP)
        visitMethodInsn(INVOKESPECIAL, "com/intellij/openapi/wm/RegisterToolWindowTask", "<init>", "()V", false)
        visitMethodInsn(
          INVOKEVIRTUAL,
          "com/intellij/openapi/wm/ToolWindowManager",
          "registerToolWindow",
          "(Lcom/intellij/openapi/wm/RegisterToolWindowTask;)Lcom/intellij/openapi/wm/ToolWindow;",
          false
        )
        val label2 = Label()
        visitLabel(label2)
        visitLineNumber(5, label2)
        visitInsn(ARETURN)
        val label3 = Label()
        visitLabel(label3)
        visitLocalVariable("\$i\$f\$registerToolWindow", "I", null, label1, label2, 2)
        visitLocalVariable("twm", "Lcom/intellij/openapi/wm/ToolWindowManager;", null, label0, label3, 1)
        visitLocalVariable("this", "Lcom/example/plugin/TwCaller;", null, label0, label3, 0)
        visitMaxs(3, 3)
        visitEnd()
      }
      classWriter.visitEnd()
      return classWriter.toByteArray()
    }
  }

  /**
   * Control case: plugin code invoking the `@OverrideOnly` method directly, without any
   * inlining markers. Such usage must always be reported.
   */
  private object DirectToolWindowCallerDump {
    fun dump(): ByteArray {
      val classWriter = ClassWriter(0)
      classWriter.visit(
        V11,
        ACC_PUBLIC or ACC_FINAL or ACC_SUPER,
        "com/example/plugin/DirectTwCaller",
        null,
        "java/lang/Object",
        null
      )
      classWriter.visitSource("DirectTwCaller.kt", null)
      classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null).apply {
        visitCode()
        visitVarInsn(ALOAD, 0)
        visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
        visitInsn(RETURN)
        visitMaxs(1, 1)
        visitEnd()
      }
      classWriter.visitMethod(
        ACC_PUBLIC or ACC_FINAL,
        "register",
        "(Lcom/intellij/openapi/wm/ToolWindowManager;)Lcom/intellij/openapi/wm/ToolWindow;",
        null,
        null
      ).apply {
        visitCode()
        val label0 = Label()
        visitLabel(label0)
        visitVarInsn(ALOAD, 1)
        visitTypeInsn(NEW, "com/intellij/openapi/wm/RegisterToolWindowTask")
        visitInsn(DUP)
        visitMethodInsn(INVOKESPECIAL, "com/intellij/openapi/wm/RegisterToolWindowTask", "<init>", "()V", false)
        visitMethodInsn(
          INVOKEVIRTUAL,
          "com/intellij/openapi/wm/ToolWindowManager",
          "registerToolWindow",
          "(Lcom/intellij/openapi/wm/RegisterToolWindowTask;)Lcom/intellij/openapi/wm/ToolWindow;",
          false
        )
        val label1 = Label()
        visitLabel(label1)
        visitInsn(ARETURN)
        visitLocalVariable("twm", "Lcom/intellij/openapi/wm/ToolWindowManager;", null, label0, label1, 1)
        visitLocalVariable("this", "Lcom/example/plugin/DirectTwCaller;", null, label0, label1, 0)
        visitMaxs(3, 3)
        visitEnd()
      }
      classWriter.visitEnd()
      return classWriter.toByteArray()
    }
  }
}
