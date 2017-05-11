package com.intellij.structure.mocks

import com.intellij.structure.ide.IdeVersion
import com.intellij.structure.impl.domain.PluginDependencyImpl
import com.intellij.structure.impl.extractor.ExtractedPluginFile
import com.intellij.structure.impl.utils.JarsUtils
import com.intellij.structure.plugin.Plugin
import com.intellij.structure.plugin.PluginCreationFail
import com.intellij.structure.plugin.PluginCreationSuccess
import com.intellij.structure.plugin.PluginManager
import com.intellij.structure.problems.MissingOptionalDependencyConfigurationFile
import com.intellij.structure.problems.PluginProblem
import com.intellij.structure.resolvers.Resolver
import org.hamcrest.CoreMatchers.*
import org.hamcrest.collection.IsIn.isIn
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Created by Sergey Patrikeev
 */
class MockPluginsTest {

  private fun getMockPluginFile(mockName: String): File {
    //if run with gradle
    var pluginFile = File("mock-plugin/build/mocks/", mockName)
    if (pluginFile.exists()) {
      return pluginFile
    }
    //if run with IDE test runner
    pluginFile = File("intellij-plugin-structure/tests/mock-plugin/build/mocks", mockName)
    Assert.assertTrue("mock plugin " + mockName + " is not found in " + pluginFile.absolutePath, pluginFile.exists())
    return pluginFile
  }


  private fun testMockConfigs(plugin: Plugin) {
    assertEquals("http://kotlinlang.org", plugin.url)
    assertEquals("Kotlin", plugin.pluginName)
    assertEquals("1.0.0-beta-1038-IJ141-17", plugin.pluginVersion)
    assertEquals("org.jetbrains.kotlin2", plugin.pluginId)

    assertEquals("vendor_email", plugin.vendorEmail)
    assertEquals("http://www.jetbrains.com", plugin.vendorUrl)
    assertEquals("JetBrains s.r.o.", plugin.vendor)

    assertEquals("Kotlin language support", plugin.description)
    assertEquals(IdeVersion.createIdeVersion("141.1009.5"), plugin.sinceBuild)
    assertEquals(IdeVersion.createIdeVersion("141.9999999"), plugin.untilBuild)

    assertEquals("change_notes", plugin.changeNotes)

  }

  private fun testMockWarnings(problems: List<PluginProblem>) {
    assertContains(problems,
        listOf(MissingOptionalDependencyConfigurationFile("plugin.xml", PluginDependencyImpl("missingDependency", true), "missingFile"))
    )
  }

  @Test
  fun `jar file packed in zip`() {
    testMockPluginStructureAndConfiguration("mock-plugin-jar-in-zip.zip", "")
  }

  @Test
  fun `classes directory packed in zip`() {
    testMockPluginStructureAndConfiguration("mock-plugin-classes-zip.zip", "classes")
  }

  @Test
  fun `plugin as directory with classes`() {
    testMockPluginStructureAndConfiguration("mock-plugin-classes", "classes")
  }

  @Test
  fun `plugin jar packed in lib directory of zip archive`() {
    testMockPluginStructureAndConfiguration("mock-plugin-lib.zip", "lib/mock-plugin.jar")
  }

  @Test
  fun `directory with lib subdirectory containing jar file`() {
    testMockPluginStructureAndConfiguration("mock-plugin-dir", "lib/mock-plugin.jar")
  }

  @Test
  fun `jar file renamed to zip archive`() {
    testMockPluginStructureAndConfiguration("mock-pluginJarAsZip.zip", "")
  }

  @Test
  @Throws(Exception::class)
  fun `single jar file`() {
    testMockPluginStructureAndConfiguration("mock-plugin.jar", "")
  }

  @Test
  @Throws(Exception::class)
  fun `plugin directory with lib containing jar file - packed in zip archive`() {
    testMockPluginStructureAndConfiguration("mock-plugin-directory-with-lib-in-zip.zip", "lib/mock-plugin.jar")
  }

  private fun testMockIdeCompatibility(plugin: Plugin) {
    //  <idea-version since-build="141.1009.5" until-build="141.9999999"/>
    checkCompatible(plugin, "141.1009.5", true)
    checkCompatible(plugin, "141.99999", true)
    checkCompatible(plugin, "142.0", false)
    checkCompatible(plugin, "141.1009.4", false)
    checkCompatible(plugin, "141", false)
  }

  private fun checkCompatible(plugin: Plugin, version: String, compatible: Boolean) {
    assertEquals(compatible, plugin.isCompatibleWithIde(IdeVersion.createIdeVersion(version)))
  }

  private fun testMockPluginStructureAndConfiguration(pluginPath: String, vararg classesPath: String) {
    val pluginFile = getMockPluginFile(pluginPath)

    val pluginCreationResult = PluginManager.getInstance().createPlugin(pluginFile, true, true)
    if (pluginCreationResult is PluginCreationFail) {
      val message = pluginCreationResult.errorsAndWarnings.joinToString(separator = "\n") { it.message }
      fail(message)
    }
    val pluginCreationSuccess = pluginCreationResult as PluginCreationSuccess
    val plugin = pluginCreationSuccess.plugin

    pluginCreationSuccess.classesResolver.use { classesResolver ->
      assertThat(classesResolver, `is`(not(nullValue())))
      assertEquals(pluginFile, plugin.originalFile)
      testMockConfigs(plugin)
      testMockWarnings(pluginCreationSuccess.warnings)
      testMockClassesFromXml(plugin)
      testMockExtensionPoints(plugin)
      testMockDependenciesAndModules(plugin)
      testMockOptDescriptors(plugin)
      testMockUnderlyingDocument(plugin)
      testMockIdeCompatibility(plugin)
      testMockClasses(classesResolver, *classesPath)
    }

    val extractedPluginPath = getExtractedPluginPath(pluginCreationSuccess.classesResolver)
    if (JarsUtils.isZip(pluginFile)) {
      assertThat(extractedPluginPath.exists(), `is`(false))
    }
  }

  private fun testMockUnderlyingDocument(plugin: Plugin) {
    val document = plugin.underlyingDocument
    val rootElement = document.rootElement
    assertNotNull(rootElement)
    assertEquals("idea-plugin", rootElement.name)
  }

  private fun testMockOptDescriptors(plugin: Plugin) {
    val optionalDescriptors = plugin.optionalDescriptors
    assertContains(optionalDescriptors.keys, listOf("extension.xml", "optionals/optional.xml", "../optionalsDir/otherDirOptional.xml", "/META-INF/referencedFromRoot.xml"))
    assertEquals(4, optionalDescriptors.size.toLong())

    assertContains(optionalDescriptors["extension.xml"]!!.allClassesReferencedFromXml, listOf("org.jetbrains.plugins.scala.project.maven.MavenWorkingDirectoryProviderImpl".replace('.', '/')))
    assertContains(optionalDescriptors["optionals/optional.xml"]!!.allClassesReferencedFromXml, listOf("com.intellij.BeanClass".replace('.', '/')))
    assertContains(optionalDescriptors["../optionalsDir/otherDirOptional.xml"]!!.allClassesReferencedFromXml, listOf("com.intellij.optional.BeanClass".replace('.', '/')))
  }

  private fun testMockDependenciesAndModules(plugin: Plugin) {
    assertEquals(6, plugin.dependencies.size.toLong())
    val dependencies = listOf(
        PluginDependencyImpl("JUnit", true),
        PluginDependencyImpl("optionalDependency", true),
        PluginDependencyImpl("otherDirOptionalDependency", true),
        PluginDependencyImpl("mandatoryDependency", false),
        PluginDependencyImpl("referenceFromRoot", true),
        PluginDependencyImpl("missingDependency", true)
    )
    assertContains(plugin.dependencies, dependencies)

    //check module dependencies
    assertEquals(listOf(PluginDependencyImpl("com.intellij.modules.mandatoryDependency", false)), plugin.moduleDependencies)
    assertEquals(hashSetOf("one_module", "two_module"), plugin.definedModules)
  }

  private fun testMockExtensionPoints(plugin: Plugin) {
    val extensions = plugin.extensions
    val keys = extensions.keys()
    assertThat("com.intellij.referenceImporter", isIn(keys))
    assertThat("org.intellij.scala.scalaTestDefaultWorkingDirectoryProvider", isIn(keys))
  }

  private fun testMockClassesFromXml(plugin: Plugin) {
    val set = plugin.allClassesReferencedFromXml
    assertContains(set, "org.jetbrains.kotlin.idea.compiler.JetCompilerManager".replace('.', '/'))
    assertContains(set, "org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages\$Extension".replace('.', '/'))
    assertContains(set, "org.jetbrains.kotlin.idea.configuration.KotlinProjectConfigurator".replace('.', '/'))
    assertContains(set, "org.jetbrains.kotlin.js.resolve.diagnostics.DefaultErrorMessagesJs".replace('.', '/'))
    assertContains(set, "org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions.UnfoldReturnToWhenIntention".replace('.', '/'))
    assertFalse(set.contains("org.jetbrains.plugins.scala.project.maven.MavenWorkingDirectoryProviderImpl".replace('.', '/')))
  }

  private fun testMockClasses(resolver: Resolver, vararg classPath: String) {
    val allClasses = resolver.allClasses
    assertEquals(4, allClasses.size.toLong())
    assertContains(allClasses, listOf("packagename/InFileClassOne", "packagename/ClassOne\$ClassOneInnerStatic", "packagename/ClassOne\$ClassOneInner", "packagename/InFileClassOne"))

    val extracted = getExtractedPluginPath(resolver).canonicalFile
    assertClassPath(resolver, extracted, *classPath)
  }

  private fun assertClassPath(resolver: Resolver, extracted: File, vararg classPath: String) {
    val pluginClassPath = resolver.classPath
    for (cp in classPath) {
      val shouldBe = File(extracted, cp)
      var found = false
      for (pcp in pluginClassPath) {
        if (pcp.canonicalPath == shouldBe.canonicalPath) {
          found = true
        }
      }
      Assert.assertTrue("The class path $shouldBe is not found", found)
    }
  }

  private fun getExtractedPluginPath(resolver: Resolver): File {
    val field = resolver.javaClass.getDeclaredField("myExtractedPluginFile")
    field.isAccessible = true
    return (field.get(resolver) as ExtractedPluginFile).actualPluginFile
  }

  private fun <T> assertContains(collection: Collection<T>, elem: T) = assertContains(collection, listOf(elem))

  private fun <T> assertContains(collection: Collection<T>, elems: List<T>) {
    for (t in elems) {
      if (!collection.contains(t)) {
        System.err.println(collection)
        throw AssertionError("Collection must contain an element " + t)
      }
    }

  }
}
