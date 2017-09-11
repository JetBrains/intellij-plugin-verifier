package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.utils.FileUtil
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.extractor.ExtractedPluginFile
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.plugin.PluginXmlUtil.getAllClassesReferencedFromXml
import com.jetbrains.plugin.structure.intellij.problems.MissingOptionalDependencyConfigurationFile
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
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

  companion object {
    fun getMockPluginFile(mockName: String): File {
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
  }


  private fun testMockConfigs(plugin: IdePlugin) {
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
        listOf(MissingOptionalDependencyConfigurationFile("plugin.xml", PluginDependencyImpl("missingDependency", true, false), "missingFile"))
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
    testMockPluginStructureAndConfiguration("mock-plugin-lib.zip", "lib/mock-plugin-1.0.jar")
  }

  @Test
  fun `directory with lib subdirectory containing jar file`() {
    testMockPluginStructureAndConfiguration("mock-plugin-dir", "lib/mock-plugin-1.0.jar")
  }

  @Test
  @Throws(Exception::class)
  fun `single jar file`() {
    testMockPluginStructureAndConfiguration("mock-plugin-1.0.jar", "")
  }

  @Test
  @Throws(Exception::class)
  fun `plugin directory with lib containing jar file - packed in zip archive`() {
    testMockPluginStructureAndConfiguration("mock-plugin-directory-with-lib-in-zip.zip", "lib/mock-plugin-1.0.jar")
  }

  private fun testMockIdeCompatibility(plugin: IdePlugin) {
    //  <idea-version since-build="141.1009.5" until-build="141.9999999"/>
    checkCompatible(plugin, "141.1009.5", true)
    checkCompatible(plugin, "141.99999", true)
    checkCompatible(plugin, "142.0", false)
    checkCompatible(plugin, "141.1009.4", false)
    checkCompatible(plugin, "141", false)
  }

  private fun checkCompatible(plugin: IdePlugin, version: String, compatible: Boolean) {
    assertEquals(compatible, plugin.isCompatibleWithIde(IdeVersion.createIdeVersion(version)))
  }

  private fun testMockPluginStructureAndConfiguration(pluginPath: String, vararg classesPath: String) {
    val pluginFile = getMockPluginFile(pluginPath)

    val pluginCreationResult = IdePluginManager.createManager().createPlugin(pluginFile)
    if (pluginCreationResult is PluginCreationFail) {
      val message = pluginCreationResult.errorsAndWarnings.joinToString(separator = "\n") { it.message }
      fail(message)
    }
    val pluginCreationSuccess = pluginCreationResult as PluginCreationSuccess
    val plugin = pluginCreationSuccess.plugin
    val classesResolver = Resolver.createPluginResolver(plugin)

    classesResolver.use {
      assertThat(classesResolver, `is`(not(nullValue())))
      assertEquals(pluginFile, plugin.originalFile)
      testMockConfigs(plugin)
      testMockWarnings(pluginCreationSuccess.warnings)
      testMockExtensionPoints(plugin)
      testMockDependenciesAndModules(plugin)
      testMockOptDescriptors(plugin)
      testMockUnderlyingDocument(plugin)
      testMockIdeCompatibility(plugin)
      testMockClasses(classesResolver, *classesPath)
    }

    val extractedPluginPath = getExtractedPluginPath(classesResolver)
    if (FileUtil.isZip(pluginFile)) {
      assertThat(extractedPluginPath.exists(), `is`(false))
    }
  }

  private fun testMockUnderlyingDocument(plugin: IdePlugin) {
    val document = plugin.underlyingDocument
    val rootElement = document.rootElement
    assertNotNull(rootElement)
    assertEquals("idea-plugin", rootElement.name)
  }

  private fun testMockOptDescriptors(plugin: IdePlugin) {
    val optionalDescriptors = plugin.optionalDescriptors
    assertContains(optionalDescriptors.keys, listOf("extension.xml", "optionals/optional.xml", "../optionalsDir/otherDirOptional.xml", "/META-INF/referencedFromRoot.xml"))
    assertEquals(4, optionalDescriptors.size.toLong())

    assertContains(getAllClassesReferencedFromXml(optionalDescriptors["extension.xml"]!!), listOf("org.jetbrains.plugins.scala.project.maven.MavenWorkingDirectoryProviderImpl".replace('.', '/')))
    assertContains(getAllClassesReferencedFromXml(optionalDescriptors["optionals/optional.xml"]!!), listOf("com.intellij.BeanClass".replace('.', '/')))
    assertContains(getAllClassesReferencedFromXml(optionalDescriptors["../optionalsDir/otherDirOptional.xml"]!!), listOf("com.intellij.optional.BeanClass".replace('.', '/')))
  }

  private fun testMockDependenciesAndModules(plugin: IdePlugin) {
    assertEquals(7, plugin.dependencies.size.toLong())
    //check plugin and module dependencies
    val dependencies = listOf(
        PluginDependencyImpl("JUnit", true, false),
        PluginDependencyImpl("optionalDependency", true, false),
        PluginDependencyImpl("otherDirOptionalDependency", true, false),
        PluginDependencyImpl("mandatoryDependency", false, false),
        PluginDependencyImpl("referenceFromRoot", true, false),
        PluginDependencyImpl("missingDependency", true, false),
        PluginDependencyImpl("com.intellij.modules.mandatoryDependency", false, true)
    )
    assertContains(plugin.dependencies, dependencies)

    assertEquals(hashSetOf("one_module", "two_module"), plugin.definedModules)
  }

  private fun testMockExtensionPoints(plugin: IdePlugin) {
    val extensions = plugin.extensions
    val keys = extensions.keys()
    assertThat("com.intellij.referenceImporter", isIn(keys))
    assertThat("org.intellij.scala.scalaTestDefaultWorkingDirectoryProvider", isIn(keys))
  }

  private fun testMockClasses(resolver: Resolver, vararg classPath: String) {
    val allClasses = resolver.allClasses.asSequence().toList()
    assertEquals(4, allClasses.size.toLong())
    assertContains(allClasses, listOf("packagename/InFileClassOne", "packagename/ClassOne\$ClassOneInnerStatic", "packagename/ClassOne\$ClassOneInner", "packagename/InFileClassOne"))

    val extracted = getExtractedPluginPath(resolver).canonicalFile
    assertClassPath(resolver, extracted, *classPath)
  }

  private fun assertClassPath(resolver: Resolver, extracted: File, vararg classPath: String) {
    val pluginClassPath = resolver.classPath
    for (cp in classPath) {
      val shouldBe = File(extracted, cp)
      val found = pluginClassPath.any { it.canonicalPath == shouldBe.canonicalPath }
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
