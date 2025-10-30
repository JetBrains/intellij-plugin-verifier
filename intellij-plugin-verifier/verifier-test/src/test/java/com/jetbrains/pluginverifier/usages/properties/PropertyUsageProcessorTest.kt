/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.properties

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.FixedClassesResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.results.problems.MissingPropertyReferenceProblem
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.tests.mocks.MockVerificationContext
import com.jetbrains.pluginverifier.tests.mocks.asm.constructorPublicNoArg
import com.jetbrains.pluginverifier.tests.mocks.asm.constructorPublicString
import com.jetbrains.pluginverifier.tests.mocks.asm.publicNoArgReturnVoid
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.BinaryClassName
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileAsm
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.MethodAsm
import net.bytebuddy.jar.asm.Opcodes.INVOKEVIRTUAL
import org.junit.Assert.*
import org.junit.Test
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import java.io.StringReader
import java.util.*

class PropertyUsageProcessorTest {
  @Test
  fun `resource bundle property is resolved`() {
    val propertyUsageProcessor = object : ApiUsageProcessor {
      override fun processMethodInvocation(
        methodReference: MethodReference,
        resolvedMethod: Method,
        instructionNode: AbstractInsnNode,
        callerMethod: Method,
        context: VerificationContext
      ) {
        DefaultPropertyChecker.checkProperty(MOCK_RESOURCE_BUNDLE, MOCK_RESOURCE_BUNDLE_KEY, context, resolvedMethod.location)
      }
    }
    with(invocationSpec()) {
      val context = mockVerificationContext()

      propertyUsageProcessor.processMethodInvocation(reference, method, instructionNode, invokerMethod, context)

      assertTrue(context.problems.isEmpty())
      assertTrue(context.warnings.isEmpty())
    }
  }

  @Test
  fun `resource bundle property is not found`() {
    val propertyUsageProcessor = object : ApiUsageProcessor {
      override fun processMethodInvocation(
        methodReference: MethodReference,
        resolvedMethod: Method,
        instructionNode: AbstractInsnNode,
        callerMethod: Method,
        context: VerificationContext
      ) {
        DefaultPropertyChecker.checkProperty(MOCK_RESOURCE_BUNDLE, "UNKNOWN_PROPERTY", context, resolvedMethod.location)
      }
    }
    with(invocationSpec()) {
      val context = mockVerificationContext()

      propertyUsageProcessor.processMethodInvocation(reference, method, instructionNode, invokerMethod, context)

      with(context.problems) {
        assertEquals(1, size)
        val problem = first()
        assertTrue(problem is MissingPropertyReferenceProblem)
        val missingPropertyReference = problem as MissingPropertyReferenceProblem
        assertEquals("UNKNOWN_PROPERTY", missingPropertyReference.propertyKey)
        assertEquals(MOCK_RESOURCE_BUNDLE, missingPropertyReference.bundleBaseName)
        assertNotNull(missingPropertyReference.usageLocation)
      }
      assertTrue(context.warnings.isEmpty())
    }
  }

  @Test
  fun `resource bundle property is resolved in deprecated resource bundle`() {
    val propertyUsageProcessor = object : ApiUsageProcessor {
      override fun processMethodInvocation(
        methodReference: MethodReference,
        resolvedMethod: Method,
        instructionNode: AbstractInsnNode,
        callerMethod: Method,
        context: VerificationContext
      ) {
        DefaultPropertyChecker.checkProperty(MOCK_RESOURCE_BUNDLE, MOCK_DEPRECATED_RESOURCE_BUNDLE_KEY, context, resolvedMethod.location)
      }
    }
    with(invocationSpec()) {
      val context = mockVerificationContext()

      propertyUsageProcessor.processMethodInvocation(reference, method, instructionNode, invokerMethod, context)

      assertTrue(context.problems.isEmpty())

      with(context.warnings) {
        assertEquals(1, size)
        val problem = first()
        assertTrue(problem is DeprecatedPropertyUsageWarning)
        val deprecatedPropertyUsage = problem as DeprecatedPropertyUsageWarning
        assertEquals(MOCK_DEPRECATED_RESOURCE_BUNDLE_KEY, deprecatedPropertyUsage.propertyKey)
        assertEquals(MOCK_RESOURCE_BUNDLE, deprecatedPropertyUsage.originalResourceBundle)
        assertEquals(MOCK_DEPRECATED_RESOURCE_BUNDLE, deprecatedPropertyUsage.deprecatedResourceBundle)
        assertNotNull(deprecatedPropertyUsage.usageLocation)
      }
    }
  }

  private fun invocationSpec(): MethodInvocationSpec {
    val linterClassName: BinaryClassName = "mock/plugin/property/Linter"
    val methodName = "<init>"
    val methodDescriptor = "(Ljava/lang/String;)V"

    val methodReference = MethodReference("mock.plugin.property.Linter", methodName, methodDescriptor)

    val classFile = ClassFileAsm(invocationTargetClassNode(linterClassName), origin)
    val invokedMethod = MethodAsm(classFile, constructorPublicString())
    val instruction = MethodInsnNode(INVOKEVIRTUAL, linterClassName, methodName, methodDescriptor)

    val invokerClassFile = ClassFileAsm(invokerClassNode("mock/Handler"), origin)
    val invokerMethod = MethodAsm(invokerClassFile, publicNoArgReturnVoid("handle"))

    return MethodInvocationSpec(invokedMethod, methodReference, instruction, invokerMethod)
  }

  private fun mockVerificationContext(): MockVerificationContext {
    val classResolver = FixedClassesResolver.create(classes = emptyList(), origin, Resolver.ReadMode.FULL, mockResourceBundles())
    return MockVerificationContext(classResolver)
  }

  private fun mockResourceBundles(): Map<String, PropertyResourceBundle> {
    return mapOf(
      MOCK_RESOURCE_BUNDLE to PropertyResourceBundle(StringReader(MOCK_RESOURCE_BUNDLE_CONTENT)),
      MOCK_DEPRECATED_RESOURCE_BUNDLE to PropertyResourceBundle(StringReader(MOCK_DEPRECATED_RESOURCE_BUNDLE_CONTENT))
    )
  }
}

data class MethodInvocationSpec(
  val method: Method,
  val reference: MethodReference,
  val instructionNode: AbstractInsnNode,
  val invokerMethod: Method
)

private fun invokerClassNode(className: BinaryClassName): ClassNode = ClassNode(ASM9).apply {
    version = V1_8 // Java 8
    access = ACC_PUBLIC
    name = className
    superName = "java/lang/Object"
    methods.add(publicNoArgReturnVoid("handle"))
  }


private fun invocationTargetClassNode(className: BinaryClassName): ClassNode = ClassNode(ASM9).apply {
  version = V1_8 // Java 8
  access = ACC_PUBLIC
  name = className
  superName = "java/lang/Object"
  methods.add(constructorPublicNoArg())
}

private val origin = object : FileOrigin {
  override val parent = null
}

private const val MOCK_RESOURCE_BUNDLE = "mock/plugin/property/Resources"
private const val MOCK_RESOURCE_BUNDLE_KEY = "name"
private const val MOCK_RESOURCE_BUNDLE_CONTENT = "$MOCK_RESOURCE_BUNDLE_KEY=mock"

private const val MOCK_DEPRECATED_RESOURCE_BUNDLE = "mock/plugin/property/TestingDeprecatedMessagesBundle"
private const val MOCK_DEPRECATED_RESOURCE_BUNDLE_KEY = "deprecatedName"
private const val MOCK_DEPRECATED_RESOURCE_BUNDLE_CONTENT = "$MOCK_DEPRECATED_RESOURCE_BUNDLE_KEY=deprecatedValue"