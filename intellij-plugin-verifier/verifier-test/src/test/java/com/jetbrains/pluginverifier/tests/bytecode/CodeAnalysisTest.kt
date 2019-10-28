package com.jetbrains.pluginverifier.tests.bytecode

import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.JarFileResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.jdk.JdkDescriptorCreator
import com.jetbrains.pluginverifier.tests.findMockPluginJarPath
import com.jetbrains.pluginverifier.tests.mocks.TestJdkDescriptorProvider.getJdkPathForTests
import com.jetbrains.pluginverifier.verifiers.extractConstantFunctionValue
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassOrNull
import org.junit.Assert
import org.junit.Test

class CodeAnalysisTest {
  @Test
  fun `evaluate constant strings`() {
    createTestResolver().use { testResolver ->
      JdkDescriptorCreator.createJdkDescriptor(getJdkPathForTests()).use { jdkDescriptor ->
        CompositeResolver.create(testResolver, jdkDescriptor.jdkResolver).use { resolver ->
          val classNode = resolver.resolveClassOrNull("mock/plugin/codeAnalysis/ConstantStrings")!!
          assertEvaluatedConstantStringValue(resolver, classNode, "constantFunctionReturn", "ConstantFunctionValue")
          assertEvaluatedConstantStringValue(resolver, classNode, "staticFieldFunctionReturn", "StaticFieldValue")
          assertEvaluatedConstantStringValue(resolver, classNode, "staticFieldConcatenatedReturn", "StaticFieldConcatenated")
          assertEvaluatedConstantStringValue(resolver, classNode, "recursiveString", null)
          assertEvaluatedConstantStringValue(resolver, classNode, "myFunction", ".constantValue")
          assertEvaluatedConstantStringValue(resolver, classNode, "myRefFunction", ".constantValue")
          assertEvaluatedConstantStringValue(resolver, classNode, "instance", ".constantValue")
          assertEvaluatedConstantStringValue(resolver, classNode, "staticConstant", "I_am_constant")
          assertEvaluatedConstantStringValue(resolver, classNode, "concat", ".constantValueConcat")
          assertEvaluatedConstantStringValue(resolver, classNode, "concat2", "prefix.constantValue.constantValue")
        }
      }
    }
  }

  private fun createTestResolver(): Resolver =
    JarFileResolver(
      findMockPluginJarPath(),
      Resolver.ReadMode.FULL,
      object : FileOrigin {
        override val parent: FileOrigin? = null
      }
    )

  private fun assertEvaluatedConstantStringValue(
    resolver: Resolver,
    classFile: ClassFile,
    methodName: String,
    expectedValue: String?
  ) {
    val method = classFile.methods.find { it.name == methodName }!!
    val string = extractConstantFunctionValue(method, resolver)
    Assert.assertEquals(expectedValue, string)
  }

}