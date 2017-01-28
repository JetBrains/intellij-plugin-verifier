package com.intellij.featureExtractor

import com.jetbrains.intellij.feature.extractor.AnalysisUtil
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.jetbrains.intellij.plugins.internal.asm.tree.MethodNode
import org.junit.Assert
import org.junit.Test

/**
 * @author Sergey Patrikeev
 */
class AnalysisUtilTest : FeatureExtractorTestBase() {
  @Test
  fun constantFunction() {
    val classNode = readClassNode("featureExtractor.common.ConstantHolder")
    val methods = classNode.methods as List<MethodNode>

    assertFunctionValueExtraction(classNode, "myFunction", methods, ".constantValue")
    assertFunctionValueExtraction(classNode, "myRefFunction", methods, ".constantValue")
    assertFunctionValueExtraction(classNode, "instance", methods, ".constantValue")
    assertFunctionValueExtraction(classNode, "staticConstant", methods, "I_am_constant")
  }

  @Test
  fun concatenation() {
    val classNode = readClassNode("featureExtractor.common.ConstantHolder")
    val methods = classNode.methods as List<MethodNode>

    assertFunctionValueExtraction(classNode, "concat", methods, ".constantValueConcat")
    assertFunctionValueExtraction(classNode, "concat2", methods, "prefix.constantValue.constantValue")
  }

  private fun assertFunctionValueExtraction(classNode: ClassNode, fn: String, methods: List<MethodNode>, value: String) {
    val m = methods.find { it.name == fn }!!
    Assert.assertEquals(value, AnalysisUtil.extractConstantFunctionValue(classNode, m, resolver))
  }

}
