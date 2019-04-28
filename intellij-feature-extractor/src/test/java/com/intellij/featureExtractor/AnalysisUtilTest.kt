package com.intellij.featureExtractor

import com.jetbrains.intellij.feature.extractor.core.AnalysisUtil
import org.junit.Assert
import org.junit.Test
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class AnalysisUtilTest : FeatureExtractorTestBase() {
  @Test
  fun constantFunction() {
    val classNode = readClassNode("featureExtractor.common.ConstantHolder")
    val methods = classNode.methods

    assertFunctionValueExtraction(classNode, "myFunction", methods, ".constantValue")
    assertFunctionValueExtraction(classNode, "myRefFunction", methods, ".constantValue")
    assertFunctionValueExtraction(classNode, "instance", methods, ".constantValue")
    assertFunctionValueExtraction(classNode, "staticConstant", methods, "I_am_constant")
  }

  @Test
  fun concatenation() {
    val classNode = readClassNode("featureExtractor.common.ConstantHolder")
    val methods = classNode.methods

    assertFunctionValueExtraction(classNode, "concat", methods, ".constantValueConcat")
    assertFunctionValueExtraction(classNode, "concat2", methods, "prefix.constantValue.constantValue")
  }

  private fun assertFunctionValueExtraction(classNode: ClassNode, fn: String, methods: List<MethodNode>, value: String) {
    val m = methods.find { it.name == fn }!!
    Assert.assertEquals(value, AnalysisUtil.extractConstantFunctionValue(classNode, m, resolver))
  }

}
