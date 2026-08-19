/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.pluginverifier.verifiers.method.KotlinMethods.isCompilerSynthesizedInterfaceBridge
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileAsm
import kotlinx.metadata.KmClass
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmConstructor
import kotlinx.metadata.KmFunction
import kotlinx.metadata.KmProperty
import kotlinx.metadata.KmType
import kotlinx.metadata.jvm.JvmMetadataVersion
import kotlinx.metadata.jvm.JvmMethodSignature
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.getterSignature
import kotlinx.metadata.jvm.setterSignature
import kotlinx.metadata.jvm.signature
import org.junit.Assert.assertFalse
import org.junit.Test
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode

/**
 * Guards the "not a compiler-synthesized interface bridge" side of [KotlinMethods].
 *
 * `@kotlin.Metadata` lists a class's source-declared members in several separate collections:
 * ordinary functions in [KmClass.functions], constructors in [KmClass.constructors] and property
 * accessors in [KmClass.properties]. Treating "absent from [KmClass.functions]" as proof that the
 * compiler synthesized a method therefore misclassifies constructors, property accessors, and
 * synthetic default-argument overloads.
 *
 * Misclassification is not cosmetic: [MethodArgumentTypesVerifier], [MethodReturnTypeVerifier] and
 * [MethodLocalVarsVerifier] all skip a method that is reported as a bridge, so a false positive
 * silently drops those API usage checks.
 */
class KotlinMethodsNonFunctionMembersTest {

  @Test
  fun `source declared constructor is not a synthesized interface bridge`() {
    val descriptor = "(L$INTERNAL_TYPE;)V"
    val method = classFileWithMetadata(
      methodNode = constructor(descriptor),
      declaredConstructors = listOf(JvmMethodSignature("<init>", descriptor))
    ).methods.single()

    assertFalse(
      "An ordinary Kotlin constructor must not be reported as a compiler-synthesized bridge",
      method.isCompilerSynthesizedInterfaceBridge()
    )
  }

  @Test
  fun `source declared property getter is not a synthesized interface bridge`() {
    val method = classFileWithMetadata(
      methodNode = propertyGetter(),
      declaredProperties = listOf(PROPERTY_NAME)
    ).methods.single()

    assertFalse(
      "An ordinary Kotlin property getter must not be reported as a compiler-synthesized bridge",
      method.isCompilerSynthesizedInterfaceBridge()
    )
  }

  @Test
  fun `source declared property setter is not a synthesized interface bridge`() {
    val method = classFileWithMetadata(
      methodNode = propertySetter(),
      declaredProperties = listOf(PROPERTY_NAME)
    ).methods.single()

    assertFalse(
      "An ordinary Kotlin property setter must not be reported as a compiler-synthesized bridge",
      method.isCompilerSynthesizedInterfaceBridge()
    )
  }

  /**
   * A function with default parameter values gets a synthetic `name$default` companion method that
   * metadata does not list. It is compiler-generated, but it is not an interface-default bridge: it
   * forwards to the real function on the *same* class, so its signature types still belong to code
   * the developer wrote and must keep being verified.
   */
  @Test
  fun `synthetic default arguments overload is not a synthesized interface bridge`() {
    val method = classFileWithMetadata(
      methodNode = defaultArgumentsOverload(),
      declaredFunctions = listOf(JvmMethodSignature(FUNCTION_NAME, "(L$INTERNAL_TYPE;)V"))
    ).methods.single()

    assertFalse(
      "A synthetic \$default overload must not be reported as a compiler-synthesized bridge",
      method.isCompilerSynthesizedInterfaceBridge()
    )
  }

  /**
   * The core of the fix: being absent from metadata is not on its own evidence of an interface
   * bridge. Without a body that forwards to an interface's default implementation, a method must not
   * be classified as one.
   */
  @Test
  fun `method absent from metadata that forwards nowhere is not a synthesized interface bridge`() {
    val method = classFileWithMetadata(
      methodNode = nonForwardingMethod(),
      declaredFunctions = emptyList()
    ).methods.single()

    assertFalse(
      "A method that does not forward to an interface default must not be reported as a bridge",
      method.isCompilerSynthesizedInterfaceBridge()
    )
  }

  /**
   * Kotlin interface delegation (`class C : I by delegate`) also synthesizes forwarding methods, but
   * they forward through the delegate with `invokeinterface` rather than to an interface's own
   * default implementation, so they are not default-method bridges.
   */
  @Test
  fun `interface delegation forwarder is not a synthesized interface bridge`() {
    val method = classFileWithMetadata(
      methodNode = delegatingForwarder(),
      declaredFunctions = emptyList(),
      interfaces = listOf(TEST_INTERFACE_NAME)
    ).methods.single()

    assertFalse(
      "A delegation forwarder must not be reported as a compiler-synthesized bridge",
      method.isCompilerSynthesizedInterfaceBridge()
    )
  }

  private fun constructor(descriptor: String): MethodNode =
    MethodNode(Opcodes.ACC_PUBLIC, "<init>", descriptor, null, null).apply {
      instructions.add(VarInsnNode(Opcodes.ALOAD, 0))
      instructions.add(MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false))
      instructions.add(InsnNode(Opcodes.RETURN))
    }

  private fun propertyGetter(): MethodNode =
    MethodNode(Opcodes.ACC_PUBLIC, GETTER_NAME, "()L$INTERNAL_TYPE;", null, null).apply {
      instructions.add(VarInsnNode(Opcodes.ALOAD, 0))
      instructions.add(InsnNode(Opcodes.ARETURN))
    }

  private fun propertySetter(): MethodNode =
    MethodNode(Opcodes.ACC_PUBLIC, SETTER_NAME, "(L$INTERNAL_TYPE;)V", null, null).apply {
      instructions.add(VarInsnNode(Opcodes.ALOAD, 0))
      instructions.add(VarInsnNode(Opcodes.ALOAD, 1))
      instructions.add(InsnNode(Opcodes.RETURN))
    }

  private fun defaultArgumentsOverload(): MethodNode =
    MethodNode(
      Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_SYNTHETIC,
      "$FUNCTION_NAME\$default",
      "(L$TEST_CLASS_NAME;L$INTERNAL_TYPE;ILjava/lang/Object;)V",
      null,
      null
    ).apply {
      instructions.add(VarInsnNode(Opcodes.ALOAD, 0))
      instructions.add(VarInsnNode(Opcodes.ALOAD, 1))
      instructions.add(
        MethodInsnNode(Opcodes.INVOKEVIRTUAL, TEST_CLASS_NAME, FUNCTION_NAME, "(L$INTERNAL_TYPE;)V", false)
      )
      instructions.add(InsnNode(Opcodes.RETURN))
    }

  private fun nonForwardingMethod(): MethodNode =
    MethodNode(Opcodes.ACC_PUBLIC, FUNCTION_NAME, "(L$INTERNAL_TYPE;)V", null, null).apply {
      instructions.add(VarInsnNode(Opcodes.ALOAD, 1))
      instructions.add(InsnNode(Opcodes.POP))
      instructions.add(InsnNode(Opcodes.RETURN))
    }

  private fun delegatingForwarder(): MethodNode =
    MethodNode(Opcodes.ACC_PUBLIC, FUNCTION_NAME, "(L$INTERNAL_TYPE;)V", null, null).apply {
      instructions.add(VarInsnNode(Opcodes.ALOAD, 0))
      instructions.add(VarInsnNode(Opcodes.ALOAD, 1))
      instructions.add(
        MethodInsnNode(Opcodes.INVOKEINTERFACE, TEST_INTERFACE_NAME, FUNCTION_NAME, "(L$INTERNAL_TYPE;)V", true)
      )
      instructions.add(InsnNode(Opcodes.RETURN))
    }

  private fun classFileWithMetadata(
    methodNode: MethodNode,
    declaredFunctions: List<JvmMethodSignature> = emptyList(),
    declaredConstructors: List<JvmMethodSignature> = emptyList(),
    declaredProperties: List<String> = emptyList(),
    interfaces: List<String> = emptyList()
  ): ClassFileAsm {
    val classNode = ClassNode(Opcodes.ASM9).apply {
      version = Opcodes.V1_8
      access = Opcodes.ACC_PUBLIC or Opcodes.ACC_SUPER
      name = TEST_CLASS_NAME
      superName = "java/lang/Object"
      this.interfaces = interfaces.toMutableList()
      methods = mutableListOf(methodNode)
      visibleAnnotations = mutableListOf(
        kotlinMetadata(declaredFunctions, declaredConstructors, declaredProperties)
      )
    }
    return ClassFileAsm(classNode, origin)
  }

  private fun kotlinMetadata(
    declaredFunctions: List<JvmMethodSignature>,
    declaredConstructors: List<JvmMethodSignature>,
    declaredProperties: List<String>
  ): AnnotationNode {
    val metadata = KotlinClassMetadata.Class(
      KmClass().apply {
        name = TEST_CLASS_NAME
        functions += declaredFunctions.map { signature ->
          KmFunction(signature.name).apply {
            this.signature = signature
            returnType = KmType().apply { classifier = KmClassifier.Class("kotlin/Unit") }
          }
        }
        constructors += declaredConstructors.map { signature ->
          KmConstructor().apply { this.signature = signature }
        }
        properties += declaredProperties.map { propertyName ->
          KmProperty(propertyName).apply {
            returnType = KmType().apply { classifier = KmClassifier.Class(INTERNAL_TYPE) }
            getterSignature = JvmMethodSignature(GETTER_NAME, "()L$INTERNAL_TYPE;")
            setterSignature = JvmMethodSignature(SETTER_NAME, "(L$INTERNAL_TYPE;)V")
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
    const val TEST_CLASS_NAME = "com/jetbrains/pluginverifier/tests/KotlinPluginService"
    const val TEST_INTERFACE_NAME = "com/jetbrains/pluginverifier/tests/KotlinDefaultInterface"
    const val INTERNAL_TYPE = "com/intellij/internal/SomeInternalPlatformType"

    const val FUNCTION_NAME = "useInternalType"
    const val PROPERTY_NAME = "thing"
    const val GETTER_NAME = "getThing"
    const val SETTER_NAME = "setThing"

    val origin = object : FileOrigin {
      override val parent = null
    }
  }
}
