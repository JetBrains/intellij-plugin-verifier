package com.intellij.featureExtractor

import com.jetbrains.pluginverifier.verifiers.extractConstantFunctionValue
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassOrNull
import org.junit.Assert
import org.junit.Test

class AnalysisUtilTest : FeatureExtractorTestBase() {

  @Test
  fun constantFunction() {
    val classFile = resolver.resolveClassOrNull("featureExtractor/common/ConstantHolder")!!
    val methods = classFile.methods.toList()

    assertFunctionValueExtraction("myFunction", methods, ".constantValue")
    assertFunctionValueExtraction("myRefFunction", methods, ".constantValue")
    assertFunctionValueExtraction("instance", methods, ".constantValue")
    assertFunctionValueExtraction("staticConstant", methods, "I_am_constant")
  }

  @Test
  fun concatenation() {
    val classFile = resolver.resolveClassOrNull("featureExtractor/common/ConstantHolder")!!
    val methods = classFile.methods.toList()

    assertFunctionValueExtraction("concat", methods, ".constantValueConcat")
    assertFunctionValueExtraction("concat2", methods, "prefix.constantValue.constantValue")
  }

  private fun assertFunctionValueExtraction(fn: String, methods: List<Method>, value: String) {
    val m = methods.find { it.name == fn }!!
    Assert.assertEquals(value, extractConstantFunctionValue(m, resolver))
  }

}
