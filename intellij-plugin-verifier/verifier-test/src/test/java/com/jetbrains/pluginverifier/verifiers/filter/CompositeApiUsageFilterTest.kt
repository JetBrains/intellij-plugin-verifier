/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.filter

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.tests.mocks.MockVerificationContext
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileAsm
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.ClassUsageType
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodNode

class CompositeApiUsageFilterTest {
  private val context = MockVerificationContext()

  private val classFile = ClassFileAsm(
    ClassNode(Opcodes.ASM9).apply {
      version = Opcodes.V1_8
      access = Opcodes.ACC_PUBLIC
      name = "com/example/Api"
      superName = "java/lang/Object"
      fields.add(FieldNode(Opcodes.ACC_PUBLIC, "field", "I", null, null))
      methods.add(MethodNode(Opcodes.ACC_PUBLIC, "method", "()V", null, null).apply {
        instructions.add(InsnNode(Opcodes.RETURN))
      })
    },
    object : FileOrigin {
      override val parent = null
    }
  )
  private val method: Method = classFile.methods.single()
  private val field: Field = classFile.fields.single()

  private val classReferenceFilter = object : ApiUsageFilter {
    override fun allow(
      classReference: ClassReference,
      invocationTarget: ClassFile,
      caller: ClassFileMember,
      usageType: ClassUsageType,
      context: VerificationContext
    ) = true
  }

  private val fieldAccessFilter = object : ApiUsageFilter {
    override fun allow(fieldReference: FieldReference, resolvedField: Field, callerMethod: Method, context: VerificationContext) = true
  }

  private val composite = CompositeApiUsageFilter(classReferenceFilter, fieldAccessFilter)

  @Test
  fun `class reference is allowed if any child filter allows it`() {
    assertTrue(composite.allow(ClassReference("com/example/Api"), classFile, method, ClassUsageType.DEFAULT, context))
  }

  @Test
  fun `field access is allowed if any child filter allows it`() {
    assertTrue(composite.allow(FieldReference("com/example/Api", "field", "I"), field, method, context))
  }

  @Test
  fun `method invocation is not allowed if no child filter allows it`() {
    assertFalse(composite.allow(method, InsnNode(Opcodes.NOP), method, context))
  }

  @Test
  fun `class reference is not allowed if no child filter allows it`() {
    val composite = CompositeApiUsageFilter(fieldAccessFilter)
    assertFalse(composite.allow(ClassReference("com/example/Api"), classFile, method, ClassUsageType.DEFAULT, context))
  }

  @Test
  fun `field access is not allowed if no child filter allows it`() {
    val composite = CompositeApiUsageFilter(classReferenceFilter)
    assertFalse(composite.allow(FieldReference("com/example/Api", "field", "I"), field, method, context))
  }
}
