/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class ClassFileAsmTest {
  @Test
  fun `named methods retain overloads duplicates and declaration order`() {
    val first = methodNode("target", "()V")
    val unrelated = methodNode("other", "()V")
    val overload = methodNode("target", "(I)V")
    val duplicate = methodNode("target", "()V")
    val classFile = classFile(first, unrelated, overload, duplicate)

    val methods = classFile.methodsNamed("target").toList()

    assertEquals(listOf(first, overload, duplicate), methods.map { (it as MethodAsm).asmNode })
    methods.forEach { assertSame(classFile, it.containingClassFile) }
    assertTrue(classFile.methodsNamed("missing").none())
  }

  @Test
  fun `named methods respect a wrapper that hides all methods`() {
    val delegate = classFile(methodNode("target", "()V"))
    val wrapper = object : ClassFile by delegate {
      override val methods: Sequence<Method>
        get() = emptySequence()
    }

    assertTrue(wrapper.methodsNamed("target").none())
  }

  @Test
  fun `named methods use only the methods exposed by a wrapper`() {
    val delegate = classFile(methodNode("target", "()V"), methodNode("target", "(I)V"))
    val visible = delegate.methods.last()
    val wrapper = object : ClassFile by delegate {
      override val methods: Sequence<Method>
        get() = sequenceOf(visible)
    }

    assertSame(visible, wrapper.methodsNamed("target").single())
    assertTrue(wrapper.methodsNamed("other").none())
  }

  private fun methodNode(methodName: String, descriptor: String) = MethodNode().apply {
    name = methodName
    desc = descriptor
  }

  private fun classFile(vararg methodNodes: MethodNode): ClassFileAsm {
    val node = ClassNode(Opcodes.ASM9).apply {
      name = "example/Test"
      methods.addAll(methodNodes)
    }
    val origin = object : FileOrigin {
      override val parent = null
    }
    return ClassFileAsm(node, origin)
  }
}
