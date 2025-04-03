package com.jetbrains.pluginverifier.tests.bytecode

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.LazyJarResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.tests.findMockPluginJarPath
import com.jetbrains.pluginverifier.verifiers.CodeAnalysis
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassOrNull
import org.junit.Assert
import org.junit.Test

class CodeAnalysisTest {
  @Test
  fun `evaluate constant strings`() {
    createTestResolver().use { resolver ->
      val classNode = resolver.resolveClassOrNull("mock/plugin/codeAnalysis/ConstantStrings")!!
      assertEvaluatedConstantStringValue(classNode, "constantFunctionReturn", "ConstantFunctionValue")
      assertEvaluatedConstantStringValue(classNode, "staticFieldFunctionReturn", "StaticFieldValue")
      assertEvaluatedConstantStringValue(classNode, "staticFieldConcatenatedReturn", "StaticFieldConcatenated")
      assertEvaluatedConstantStringValue(classNode, "recursiveString", null)
      assertEvaluatedConstantStringValue(classNode, "myFunction", ".constantValue")
      assertEvaluatedConstantStringValue(classNode, "myRefFunction", ".constantValue")
      assertEvaluatedConstantStringValue(classNode, "instance", ".constantValue")
      assertEvaluatedConstantStringValue(classNode, "staticConstant", "I_am_constant")
      assertEvaluatedConstantStringValue(classNode, "finalStaticInitConstant", "staticInitConstant")
      assertEvaluatedConstantStringValue(classNode, "directRecursion", null)
    }
  }

  private fun createTestResolver(): Resolver =
    LazyJarResolver(
      findMockPluginJarPath(),
      Resolver.ReadMode.FULL,
      object : FileOrigin {
        override val parent: FileOrigin? = null
      }
    )

  private fun assertEvaluatedConstantStringValue(
    classFile: ClassFile,
    methodName: String,
    expectedValue: String?
  ) {
    val method = classFile.methods.find { it.name == methodName }!!
    val string = CodeAnalysis().evaluateConstantFunctionValue(method)
    Assert.assertEquals(expectedValue, string)
  }

}