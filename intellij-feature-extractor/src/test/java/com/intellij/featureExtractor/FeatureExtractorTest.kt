package com.intellij.featureExtractor

import com.intellij.structure.impl.resolvers.FilesResolver
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.intellij.feature.extractor.*
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.jetbrains.intellij.plugins.internal.asm.tree.MethodNode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * @author Sergey Patrikeev
 */
class FeatureExtractorTest {

  private lateinit var resolver: Resolver

  @Before
  fun setUp() {
    resolver = FilesResolver("Test class files", File("."))
  }

  @After
  fun tearDown() {
    resolver.close()
  }

  @Test
  fun constant() {
    assertExtractFacets("featureExtractor.Constant", listOf("thisIsStringId"))
  }

  @Test
  fun constant2() {
    assertExtractFacets("featureExtractor.Constant2", listOf("thisIsStringId2"))
  }


  @Test
  fun finalField() {
    assertExtractFacets("featureExtractor.FinalField", listOf("thisIsStringId"))
  }

  private fun assertExtractFacets(className: String, listOf: List<String>) {
    val node = readClassNode(className)
    val extractor = FacetTypeExtractor(resolver)
    val facetTypeId = extractor.extract(node).features
    assertEquals(listOf, facetTypeId)
  }


  @Test
  fun fileTypeInstance() {
    assertExtractFileTypes("featureExtractor.ByFileTypeFactory", listOf("*.mySomeExtension"))
  }

  private fun assertExtractFileTypes(className: String, extensions: List<String>) {
    val node = readClassNode(className)
    val list = FileTypeExtractor(resolver).extract(node).features
    assertEquals(extensions, list)
  }

  @Test
  fun nameMatcher() {
    assertExtractFileTypes("featureExtractor.MatcherFileTypeFactory", listOf("firstExactName", "secondExactName", "*.nmextension"))
  }


  private fun readClassNode(className: String): ClassNode = resolver.findClass(className.replace('.', '/'))

  @Test
  fun constantFileType() {
    assertExtractFileTypes("featureExtractor.ConstantFileTypeFactory", listOf("*..someExtension"))
  }

  @Test
  fun constantFunctionFileType() {
    assertExtractFileTypes("featureExtractor.ConstantFunctionFileTypeFactory", listOf("*..constantValue"))
  }

  @Test
  fun constantFunction() {
    val classNode = readClassNode("featureExtractor.ConstantHolder")
    val methods = classNode.methods as List<MethodNode>

    assertFunctionValueExtraction(classNode, "myFunction", methods, ".constantValue")
    assertFunctionValueExtraction(classNode, "myRefFunction", methods, ".constantValue")
    assertFunctionValueExtraction(classNode, "instance", methods, ".constantValue")
    assertFunctionValueExtraction(classNode, "staticConstant", methods, "I_am_constant")
  }

  @Test
  fun concatenation() {
    val classNode = readClassNode("featureExtractor.ConstantHolder")
    val methods = classNode.methods as List<MethodNode>

    assertFunctionValueExtraction(classNode, "concat", methods, ".constantValueConcat")
    assertFunctionValueExtraction(classNode, "concat2", methods, "prefix.constantValue.constantValue")
  }

  private fun assertFunctionValueExtraction(classNode: ClassNode, fn: String, methods: List<MethodNode>, value: String) {
    val m = methods.find { it.name == fn }!!
    assertEquals(value, AnalysisUtil.extractConstantFunctionValue(classNode, m, resolver))
  }

  private fun assertExtractConfiguration(className: String, configuration: String) {
    val node = readClassNode(className)
    val actual = RunConfigurationExtractor(resolver).extract(node).features
    assertEquals(listOf(configuration), actual)
  }

  @Test
  fun constantConfiguration() {
    assertExtractConfiguration("featureExtractor.ConstantConfigurationType", "runConfiguration")
  }

  @Test
  fun baseConfiguration() {
    assertExtractConfiguration("featureExtractor.ConfigurationTypeBaseImplementor", "ConfigurationId")
  }

  @Test
  fun staticClinitConstant() {
    assertExtractFileTypes("featureExtractor.StaticInitConstantFileTypeFactory", listOf("*.initialValue"))
  }

  private fun assertExtractArtifactType(className: String, artifactTypes: List<String>) {
    val node = readClassNode(className)
    val list = ArtifactTypeExtractor(resolver).extract(node).features
    assertEquals(artifactTypes, list)
  }

  @Test
  fun directArtifact() {
    assertExtractArtifactType("featureExtractor.DirectArtifactType", listOf("ArtifactId"))
  }

  @Test
  fun indirectArtifact() {
    assertExtractArtifactType("featureExtractor.IndirectArtifactType", listOf("IndirectArtifactId"))
  }
}