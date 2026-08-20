/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.pluginverifier.verifiers.method.KotlinMethods.isCompilerSynthesizedInterfaceBridge
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileAsm
import kotlinx.metadata.KmClass
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmFunction
import kotlinx.metadata.KmType
import kotlinx.metadata.jvm.JvmMetadataVersion
import kotlinx.metadata.jvm.JvmMethodSignature
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.signature
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

class KotlinMethodsTest {

  @Test
  fun `Kotlin 1_3 default behavior - compiler synthesized DefaultImpls bridge is detected`() {
    val method = classFileWithMetadata(
      methodNode = legacyDefaultImplsBridge("defaultMethod"),
      declaredFunctions = emptyList()
    ).methods.single()

    assertTrue(method.isCompilerSynthesizedInterfaceBridge())
  }

  @Test
  fun `Kotlin 1_3 inherited default behavior - compiler synthesized DefaultImpls bridge is detected`() {
    val method = classFileWithMetadata(
      methodNode = legacyDefaultImplsBridge("defaultMethod"),
      declaredFunctions = emptyList(),
      interfaces = listOf(TEST_CHILD_INTERFACE_NAME)
    ).methods.single()

    assertTrue(method.isCompilerSynthesizedInterfaceBridge())
  }

  @Test
  fun `jvm-default enable compatibility - compiler synthesized interface default bridge is detected`() {
    val method = classFileWithMetadata(
      methodNode = jvmDefaultBridge("defaultMethod"),
      declaredFunctions = emptyList()
    ).methods.single()

    assertTrue(method.isCompilerSynthesizedInterfaceBridge())
  }

  @Test
  fun `jvm-default enable compatibility inherited default - compiler synthesized interface default bridge is detected`() {
    val method = classFileWithMetadata(
      methodNode = jvmDefaultBridge("defaultMethod"),
      declaredFunctions = emptyList(),
      interfaces = listOf(TEST_CHILD_INTERFACE_NAME)
    ).methods.single()

    assertTrue(method.isCompilerSynthesizedInterfaceBridge())
  }

  @Test
  fun `jvm-default no-compatibility - declared interface default method is not a synthesized bridge`() {
    val methodName = "defaultMethod"
    val method = classFileWithMetadata(
      methodNode = interfaceDefaultMethod(methodName),
      declaredFunctions = listOf(JvmMethodSignature(methodName, "()V")),
      classAccess = Opcodes.ACC_PUBLIC or Opcodes.ACC_INTERFACE or Opcodes.ACC_ABSTRACT,
    ).methods.single()

    assertFalse(method.isCompilerSynthesizedInterfaceBridge())
  }

  @Test
  fun `inherited default explicitly overridden in source is not detected as compiler synthesized bridge`() {
    val methodName = "defaultMethod"
    val method = classFileWithMetadata(
      methodNode = jvmDefaultBridge(methodName),
      declaredFunctions = listOf(JvmMethodSignature(methodName, "()V")),
      interfaces = listOf(TEST_CHILD_INTERFACE_NAME)
    ).methods.single()

    assertFalse(method.isCompilerSynthesizedInterfaceBridge())
  }

  @Test
  fun `legacy JvmDefault annotated interface method is not detected as compiler synthesized bridge`() {
    val methodName = "defaultMethod"
    val method = classFileWithMetadata(
      methodNode = legacyJvmDefaultAnnotatedInterfaceMethod(methodName),
      declaredFunctions = listOf(JvmMethodSignature(methodName, "()V")),
      classAccess = Opcodes.ACC_PUBLIC or Opcodes.ACC_INTERFACE or Opcodes.ACC_ABSTRACT,
    ).methods.single()

    assertTrue(method.annotations.any { it.desc == "Lkotlin/jvm/JvmDefault;" })
    assertFalse(method.isCompilerSynthesizedInterfaceBridge())
  }

  @Test
  fun `developer declared override is not detected as compiler synthesized bridge`() {
    val methodName = "defaultMethod"
    val method = classFileWithMetadata(
      methodNode = jvmDefaultBridge(methodName),
      declaredFunctions = listOf(JvmMethodSignature(methodName, "()V"))
    ).methods.single()

    assertFalse(method.isCompilerSynthesizedInterfaceBridge())
  }

  @Test
  fun `abstract Kotlin method is not detected as compiler synthesized bridge`() {
    val methodName = "abstractMethod"
    val method = classFileWithMetadata(
      methodNode = abstractMethod(methodName),
      declaredFunctions = emptyList()
    ).methods.single()

    assertFalse(method.isCompilerSynthesizedInterfaceBridge())
  }

  @Test
  fun `non Kotlin method is not detected as compiler synthesized bridge`() {
    val classFile = classFile(plainMethod("javaMethod"))
    val method = classFile.methods.single()

    assertFalse(method.isCompilerSynthesizedInterfaceBridge())
  }

  /**
   * Builds a JVM class that looks like a Kotlin class to [KotlinMethods]:
   * it contains exactly [methodNode], and its `@kotlin.Metadata` lists exactly [declaredFunctions].
   *
   * This lets the tests model the important Kotlin source distinction:
   * a method listed in metadata is something the source class declared, while a concrete method that
   * exists in bytecode but is missing from metadata is something the compiler synthesized.
   *
   * For example, the referenced interface in these samples looks like this:
   *
   * ```
   * interface KotlinDefaultInterface {
   *   fun defaultMethod() {
   *     // default implementation
   *   }
   * }
   * ```
   *
   * This models a source-declared override:
   *
   * ```
   * class KotlinDefaultMethodOwner : KotlinDefaultInterface {
   *   override fun defaultMethod() = super.defaultMethod()
   * }
   * ```
   *
   * by passing a concrete `defaultMethod` [MethodNode] and
   * `declaredFunctions = listOf(JvmMethodSignature("defaultMethod", "()V"))`.
   *
   * The synthesized bridge case uses the same concrete [MethodNode], but passes
   * `declaredFunctions = emptyList()` to model Kotlin source that did not declare the override:
   *
   * ```
   * class KotlinDefaultMethodOwner : KotlinDefaultInterface
   * ```
   */
  private fun classFileWithMetadata(
    methodNode: MethodNode,
    declaredFunctions: List<JvmMethodSignature>,
    classAccess: Int = Opcodes.ACC_PUBLIC or Opcodes.ACC_SUPER,
    interfaces: List<String> = emptyList()
  ): ClassFileAsm = classFile(methodNode, interfaces).also { classFile ->
    classFile.asmNode.access = classAccess
    classFile.asmNode.visibleAnnotations = mutableListOf(kotlinMetadata(declaredFunctions))
  }

  /**
   * Builds the minimal ASM-backed [ClassFileAsm] wrapper used by the production verifier code.
   *
   * In source terms this is a simple class such as:
   *
   * ```
   * interface KotlinDefaultInterface {
   *   fun defaultMethod() {
   *     // default implementation
   *   }
   * }
   *
   * class KotlinDefaultMethodOwner : KotlinDefaultChildInterface {
   *   // methodNode goes here
   * }
   * ```
   *
   * If [interfaces] contains `KotlinDefaultChildInterface`, the generated bytecode corresponds to
   * that `: KotlinDefaultChildInterface` clause. If [interfaces] is empty, the generated bytecode
   * corresponds to:
   *
   * ```
   * class KotlinDefaultMethodOwner {
   *   // methodNode goes here
   * }
   * ```
   *
   * The optional [interfaces] list is only there to make inherited-default scenarios visible in the
   * class shape. The detector itself does not walk those interfaces; it classifies the concrete
   * [MethodNode] that the Kotlin compiler has already emitted into this class.
   */
  private fun classFile(methodNode: MethodNode, interfaces: List<String> = emptyList()): ClassFileAsm {
    val classNode = ClassNode(Opcodes.ASM9).apply {
      version = Opcodes.V1_8
      access = Opcodes.ACC_PUBLIC or Opcodes.ACC_SUPER
      name = TEST_CLASS_NAME
      superName = "java/lang/Object"
      this.interfaces = interfaces.toMutableList()
      methods = mutableListOf(methodNode)
    }
    return ClassFileAsm(classNode, origin)
  }

  /**
   * Models old Kotlin default-interface lowering, roughly:
   *
   * ```
   * interface KotlinDefaultInterface {
   *   fun defaultMethod() { ... }
   * }
   *
   * class KotlinDefaultMethodOwner : KotlinDefaultInterface
   * ```
   *
   * Old Kotlin compilers emitted a concrete method into the implementing class that forwards to the
   * static helper `KotlinDefaultInterface$DefaultImpls.defaultMethod(this)`.
   *
   * In Java-like pseudocode, the emitted owner class method looks like this:
   *
   * ```
   * public final class KotlinDefaultMethodOwner implements KotlinDefaultInterface {
   *   public void defaultMethod() {
   *     KotlinDefaultInterface.DefaultImpls.defaultMethod(this);
   *   }
   * }
   * ```
   *
   * The Kotlin source did not declare `defaultMethod()` in `KotlinDefaultMethodOwner`, so the test
   * omits this method from the owner's `@kotlin.Metadata` function list.
   */
  private fun legacyDefaultImplsBridge(methodName: String): MethodNode =
    MethodNode(Opcodes.ACC_PUBLIC, methodName, "()V", null, null).apply {
      instructions.add(VarInsnNode(Opcodes.ALOAD, 0))
      instructions.add(
        MethodInsnNode(
          Opcodes.INVOKESTATIC,
          "$TEST_INTERFACE_NAME\$DefaultImpls",
          methodName,
          "(L$TEST_INTERFACE_NAME;)V",
          false
        )
      )
      instructions.add(InsnNode(Opcodes.RETURN))
    }

  /**
   * Models newer compatibility lowering for an inherited interface default method, roughly:
   *
   * ```
   * interface KotlinDefaultInterface {
   *   fun defaultMethod() { ... }
   * }
   *
   * class KotlinDefaultMethodOwner : KotlinDefaultInterface
   * ```
   *
   * In compatibility mode the compiler can emit an implementing-class method whose body forwards
   * directly to the interface default via `invokespecial KotlinDefaultInterface.defaultMethod()`.
   * A developer-written `override fun defaultMethod() = super.defaultMethod()` may have the same
   * bytecode, so metadata is what distinguishes source-declared overrides from synthesized bridges.
   */
  private fun jvmDefaultBridge(methodName: String): MethodNode =
    MethodNode(Opcodes.ACC_PUBLIC, methodName, "()V", null, null).apply {
      instructions.add(VarInsnNode(Opcodes.ALOAD, 0))
      instructions.add(
        MethodInsnNode(
          Opcodes.INVOKESPECIAL,
          TEST_INTERFACE_NAME,
          methodName,
          "()V",
          true
        )
      )
      instructions.add(InsnNode(Opcodes.RETURN))
    }

  /**
   * Models `-jvm-default=no-compatibility`, where the default implementation is the interface
   * method itself:
   *
   * ```
   * interface KotlinDefaultInterface {
   *   fun defaultMethod() { ... }
   * }
   * ```
   *
   * There is no synthetic implementing-class bridge in this row, so the detector should not classify
   * this declared interface method as synthesized.
   */
  private fun interfaceDefaultMethod(methodName: String): MethodNode =
    MethodNode(Opcodes.ACC_PUBLIC, methodName, "()V", null, null).apply {
      instructions.add(InsnNode(Opcodes.RETURN))
    }

  /**
   * Models the legacy experimental source form:
   *
   * ```
   * interface KotlinDefaultInterface {
   *   @JvmDefault
   *   fun defaultMethod() { ... }
   * }
   * ```
   *
   * Unlike `@JvmDefaultWithCompatibility` and `@JvmDefaultWithoutCompatibility`, this old annotation
   * can be runtime-visible in bytecode. It still marks a source-declared interface method, not a
   * compiler-synthesized bridge in an implementing class.
   */
  private fun legacyJvmDefaultAnnotatedInterfaceMethod(methodName: String): MethodNode =
    interfaceDefaultMethod(methodName).apply {
      visibleAnnotations = mutableListOf(AnnotationNode("Lkotlin/jvm/JvmDefault;"))
    }

  /**
   * Models an ordinary Java method:
   *
   * ```
   * class KotlinDefaultMethodOwner {
   *   public void javaMethod() {}
   * }
   * ```
   *
   * With no `@kotlin.Metadata` on the containing class, Kotlin-specific bridge detection must stay
   * off.
   */
  private fun plainMethod(methodName: String): MethodNode =
    MethodNode(Opcodes.ACC_PUBLIC, methodName, "()V", null, null).apply {
      instructions.add(InsnNode(Opcodes.RETURN))
    }

  /**
   * Models a Kotlin abstract declaration:
   *
   * ```
   * abstract class KotlinDefaultMethodOwner {
   *   abstract fun abstractMethod()
   * }
   * ```
   *
   * Abstract methods have no bytecode body, so they cannot be synthesized forwarding bridges.
   */
  private fun abstractMethod(methodName: String): MethodNode =
    MethodNode(
      Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT,
      methodName,
      "()V",
      null,
      null
    )

  /**
   * Creates the `@kotlin.Metadata` annotation that Kotlin would attach to the containing class.
   *
   * The only part relevant to these tests is the function list. If a JVM method signature appears
   * here, it means the Kotlin source declared that function in this class. If a concrete method is
   * present in bytecode but absent from this list, [KotlinMethods.isCompilerSynthesizedInterfaceBridge]
   * treats it as compiler-generated.
   */
  private fun kotlinMetadata(declaredFunctions: List<JvmMethodSignature>): AnnotationNode {
    val metadata = KotlinClassMetadata.Class(
      KmClass().apply {
        name = TEST_CLASS_NAME
        functions += declaredFunctions.map { signature ->
          KmFunction(signature.name).apply {
            this.signature = signature
            returnType = KmType().apply {
              classifier = KmClassifier.Class("kotlin/Unit")
            }
          }
        }
      },
      JvmMetadataVersion.LATEST_STABLE_SUPPORTED,
      0
    ).write()

    return AnnotationNode("Lkotlin/Metadata;").apply {
      values = mutableListOf(
        "k", metadata.kind,
        "mv", metadata.metadataVersion.toList(),
        "d1", metadata.data1.toList(),
        "d2", metadata.data2.toList()
      )
    }
  }

  private companion object {
    const val TEST_CLASS_NAME = "com/jetbrains/pluginverifier/tests/KotlinDefaultMethodOwner"
    const val TEST_INTERFACE_NAME = "com/jetbrains/pluginverifier/tests/KotlinDefaultInterface"
    const val TEST_CHILD_INTERFACE_NAME = "com/jetbrains/pluginverifier/tests/KotlinDefaultChildInterface"

    val origin = object : FileOrigin {
      override val parent = null
    }
  }
}
