package com.intellij.featureExtractor

import com.jetbrains.intellij.feature.extractor.ExtensionPoint
import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.intellij.feature.extractor.extractor.ModuleTypeExtractor
import org.junit.Assert.assertEquals
import org.junit.Test

class ModuleTypeTest : FeatureExtractorTestBase() {

  private fun assertModuleIdFound(className: String, expectedModuleIds: List<String>) {
    resetPluginExtensionPoint(ExtensionPoint.MODULE_TYPE, className)
    val features = ModuleTypeExtractor().extract(plugin, resolver)
    assertEquals(
      listOf(ExtensionPointFeatures(ExtensionPoint.MODULE_TYPE, expectedModuleIds)),
      features
    )
  }

  @Test
  fun `explicit constant passed to super constructor`() {
    assertModuleIdFound("featureExtractor.moduleType.ExplicitConstant", listOf("MODULE_ID"))
  }

  @Test
  fun `static final constant passed to super constructor`() {
    assertModuleIdFound("featureExtractor.moduleType.StaticConstant", listOf("MODULE_ID"))
  }

  @Test
  fun `empty constructor delegates default module id value to another constructor`() {
    assertModuleIdFound("featureExtractor.moduleType.DelegatedFromEmptyConstructor", listOf("MODULE_ID"))
  }

  @Test
  fun `one arbitrary constructor delegates to another constructor`() {
    assertModuleIdFound("featureExtractor.moduleType.DelegatedFromOtherConstructorWithExtractArgs", listOf("MODULE_ID"))
  }

  @Test
  fun `module type extends base module`() {
    assertModuleIdFound("featureExtractor.moduleType.DerivedModuleType", listOf("BASE_MODULE_ID"))
  }


}