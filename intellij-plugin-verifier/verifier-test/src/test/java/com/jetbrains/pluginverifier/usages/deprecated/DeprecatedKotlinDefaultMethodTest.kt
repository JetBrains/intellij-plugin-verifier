/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.deprecated

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.pluginverifier.tests.mocks.MockVerificationContext
import com.jetbrains.pluginverifier.usages.internal.classDump.`TestInterface$DefaultImplsDump`
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.method.KotlinMethods.isKotlinDefaultMethod
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileAsm
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode

class DeprecatedKotlinDefaultMethodTest {
  private val verificationContext: VerificationContext = MockVerificationContext()

  @Test
  fun `invocation of a DefaultImpls-owned method is ignored by KotlinDefaultImplsUsageFilter`() {
    val classNode = ClassNode(Opcodes.ASM7)
    ClassReader(`TestInterface$DefaultImplsDump`.dump()).accept(classNode, 0)
    val classFile = ClassFileAsm(classNode, origin)

    val internalMethod = classFile.methods.find { it.name == "internalMethod" }
    assertNotNull(internalMethod)
    internalMethod!!

    val filter = KotlinDefaultImplsUsageFilter()
    val noopInstruction = InsnNode(Opcodes.NOP)
    assertTrue(filter.allow(internalMethod, noopInstruction, internalMethod, verificationContext))
  }

  @Test
  fun `invocation of a regular method is not ignored by KotlinDefaultImplsUsageFilter`() {
    val regularMethodClassFile = ClassFileAsm(regularClassNode(), origin)
    val regularMethod = regularMethodClassFile.methods.find { it.name == "bar" }
    assertNotNull(regularMethod)
    regularMethod!!

    val filter = KotlinDefaultImplsUsageFilter()
    val noopInstruction = InsnNode(Opcodes.NOP)
    assertFalse(filter.allow(regularMethod, noopInstruction, regularMethod, verificationContext))
  }

  @Test
  fun `Kotlin compiler-generated DefaultImpls-forwarding stub is detected`() {
    val classFile = ClassFileAsm(defaultImplsForwardingStubClassNode(), origin)
    val stub = classFile.methods.find { it.name == "foo" }
    assertNotNull(stub)
    stub!!

    assertTrue(stub.isKotlinDefaultMethod())
  }

  @Test
  fun `a hand-written override with real logic is not detected as a DefaultImpls-forwarding stub`() {
    val classFile = ClassFileAsm(genuineOverrideClassNode(), origin)
    val override = classFile.methods.find { it.name == "foo" }
    assertNotNull(override)
    override!!

    assertFalse(override.isKotlinDefaultMethod())
  }

  private fun regularClassNode(): ClassNode = ClassNode(Opcodes.ASM9).apply {
    version = Opcodes.V1_8
    access = Opcodes.ACC_PUBLIC
    name = "mock/plugin/deprecated/RegularClass"
    superName = "java/lang/Object"
    methods.add(MethodNode().apply {
      access = Opcodes.ACC_PUBLIC
      name = "bar"
      desc = "()V"
      instructions.add(InsnNode(Opcodes.RETURN))
    })
  }

  private fun defaultImplsForwardingStubClassNode(): ClassNode = ClassNode(Opcodes.ASM9).apply {
    version = Opcodes.V1_8
    access = Opcodes.ACC_PUBLIC
    name = "mock/plugin/deprecated/NoOverrideOfDeprecatedDefaultMethod"
    superName = "java/lang/Object"
    interfaces = listOf("deprecated/DeprecatedDefaultMethodInterface")
    visibleAnnotations = listOf(AnnotationNode("Lkotlin/Metadata;"))
    methods.add(MethodNode().apply {
      access = Opcodes.ACC_PUBLIC
      name = "foo"
      desc = "()Ljava/lang/String;"
      instructions.add(VarInsnNode(Opcodes.ALOAD, 0))
      instructions.add(
        MethodInsnNode(
          Opcodes.INVOKESTATIC,
          "deprecated/DeprecatedDefaultMethodInterface\$DefaultImpls",
          "foo",
          "(Ldeprecated/DeprecatedDefaultMethodInterface;)Ljava/lang/String;",
          false
        )
      )
      instructions.add(InsnNode(Opcodes.ARETURN))
    })
  }

  private fun genuineOverrideClassNode(): ClassNode = ClassNode(Opcodes.ASM9).apply {
    version = Opcodes.V1_8
    access = Opcodes.ACC_PUBLIC
    name = "mock/plugin/deprecated/ExplicitOverrideOfDeprecatedDefaultMethod"
    superName = "java/lang/Object"
    interfaces = listOf("deprecated/DeprecatedDefaultMethodInterface")
    visibleAnnotations = listOf(AnnotationNode("Lkotlin/Metadata;"))
    methods.add(MethodNode().apply {
      access = Opcodes.ACC_PUBLIC
      name = "foo"
      desc = "()Ljava/lang/String;"
      instructions.add(LdcInsnNode("overridden"))
      instructions.add(InsnNode(Opcodes.ARETURN))
    })
  }

  private val origin = object : FileOrigin {
    override val parent = null
  }
}
