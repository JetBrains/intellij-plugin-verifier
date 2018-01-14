package com.intellij.featureExtractor

import com.jetbrains.intellij.feature.extractor.core.ModuleTypeExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleTypeTest : FeatureExtractorTestBase() {

  private fun assertModuleIdFound(className: String, moduleId: String) {
    val node = readClassNode(className)
    val result = ModuleTypeExtractor(resolver).extract(node)
    val list = result.featureNames
    assertTrue(result.extractedAll)
    assertEquals(1, list.size)
    assertEquals(moduleId, list[0])
  }

  @Test
  fun `explicit constant passed to super constructor`() {
    assertModuleIdFound("featureExtractor.moduleType.ExplicitConstant", "MODULE_ID")
  }

  @Test
  fun `static final constant passed to super constructor`() {
    assertModuleIdFound("featureExtractor.moduleType.StaticConstant", "MODULE_ID")
  }

  @Test
  fun `empty constructor delegates default module id value to another constructor`() {
    assertModuleIdFound("featureExtractor.moduleType.DelegatedFromEmptyConstructor", "MODULE_ID")
  }

  @Test
  fun `one arbitrary constructor delegates to another constructor`() {
    assertModuleIdFound("featureExtractor.moduleType.DelegatedFromOtherConstructorWithExtractArgs", "MODULE_ID")
  }

  @Test
  fun `module type extends base module`() {
    assertModuleIdFound("featureExtractor.moduleType.DerivedModuleType", "BASE_MODULE_ID")
  }


}