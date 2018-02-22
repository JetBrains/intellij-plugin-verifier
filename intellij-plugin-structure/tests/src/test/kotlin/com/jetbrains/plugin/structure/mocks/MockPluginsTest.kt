package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.classes.resolvers.JarFileResolver
import com.jetbrains.plugin.structure.intellij.classes.locator.ClassesDirectoryKey
import com.jetbrains.plugin.structure.intellij.classes.locator.CompileServerExtensionKey
import com.jetbrains.plugin.structure.intellij.classes.locator.JarPluginKey
import com.jetbrains.plugin.structure.intellij.classes.locator.LibDirectoryKey
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesFinder
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.plugin.PluginXmlUtil.getAllClassesReferencedFromXml
import com.jetbrains.plugin.structure.intellij.problems.MissingOptionalDependencyConfigurationFile
import com.jetbrains.plugin.structure.intellij.problems.PluginZipContainsMultipleFiles
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.hamcrest.collection.IsIn.isIn
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Created by Sergey Patrikeev
 */
class MockPluginsTest : BaseMockPluginTest() {
  override fun getMockPluginBuildDirectory(): File = File("mock-plugin").resolve("build").resolve("mocks")

  @JvmField
  @Rule
  var tempFolder: TemporaryFolder = TemporaryFolder()

  @Test
  fun `invalid plugin with classes packed in a root of a zip`() {
    val brokenZipFile = getMockPluginFile("invalid-mock-pluginJarAsZip.zip")
    InvalidPluginsTest.assertExpectedProblems(brokenZipFile, listOf(PluginZipContainsMultipleFiles(brokenZipFile, listOf("META-INF", "icons", "optionalsDir", "packagename"))))
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
    testMockPluginStructureAndConfiguration("mock-plugin-jar-in-zip.zip", "", false)
  }

  @Test
  fun `classes directory packed in zip`() {
    testMockPluginStructureAndConfiguration("mock-plugin-classes-zip.zip", "classes", true)
  }

  @Test
  fun `plugin as directory with classes`() {
    testMockPluginStructureAndConfiguration("mock-plugin-classes", "classes", true)
  }

  @Test
  fun `plugin jar packed in lib directory of zip archive`() {
    testMockPluginStructureAndConfiguration("mock-plugin-lib.zip", "lib/mock-plugin-1.0.jar", true)
  }

  @Test
  fun `directory with lib subdirectory containing jar file`() {
    testMockPluginStructureAndConfiguration("mock-plugin-dir", "lib/mock-plugin-1.0.jar", true)
  }

  @Test
  @Throws(Exception::class)
  fun `single jar file`() {
    testMockPluginStructureAndConfiguration("mock-plugin-1.0.jar", "", false)
  }

  @Test
  @Throws(Exception::class)
  fun `plugin directory with lib containing jar file - packed in zip archive`() {
    testMockPluginStructureAndConfiguration("mock-plugin-directory-with-lib-in-zip.zip", "lib/mock-plugin-1.0.jar", true)
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

  private fun testMockPluginStructureAndConfiguration(pluginPath: String, classPath: String, hasLibDirectory: Boolean) {
    val pluginFile = getMockPluginFile(pluginPath)

    val extractDirectory = tempFolder.newFolder()
    val pluginCreationResult = IdePluginManager.createManager(extractDirectory).createPlugin(pluginFile)
    if (pluginCreationResult is PluginCreationFail) {
      val message = pluginCreationResult.errorsAndWarnings.joinToString(separator = "\n") { it.message }
      fail(message)
    }
    val pluginCreationSuccess = pluginCreationResult as PluginCreationSuccess
    val plugin = pluginCreationSuccess.plugin

    assertEquals(pluginFile, plugin.originalFile)
    testMockConfigs(plugin)
    testMockWarnings(pluginCreationSuccess.warnings)
    testMockExtensionPoints(plugin)
    testMockDependenciesAndModules(plugin)
    testMockOptDescriptors(plugin)
    testMockUnderlyingDocument(plugin)
    testMockIdeCompatibility(plugin)
    testMockPluginClasses(plugin, classPath, hasLibDirectory)
  }

  private fun testMockPluginClasses(plugin: IdePlugin, classPath: String, hasLibDirectory: Boolean) {
    assertNotNull(plugin.originalFile)

    val extractDirectory = tempFolder.newFolder()
    assertTrue(extractDirectory.listFiles().isEmpty())

    IdePluginClassesFinder.findPluginClasses(plugin, extractDirectory, listOf(CompileServerExtensionKey)).use { locationsContainer ->
      testMockClasses(locationsContainer, hasLibDirectory, classPath)
    }

    //Assert the extracted file was removed on Resolver close
    assertTrue(extractDirectory.listFiles().isEmpty())
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

  private fun testMockClasses(locationsContainer: IdePluginClassesLocations, hasLibDirectory: Boolean, classPath: String) {
    if (hasLibDirectory) {
      testCompileServerJars(locationsContainer)
    }

    val mainResolvers = listOf(ClassesDirectoryKey, LibDirectoryKey, JarPluginKey).mapNotNull { locationsContainer.getResolver(it) }
    val allClasses = mainResolvers.flatMap { it.allClasses }.toSet()
    assertSetsEqual(setOf("packagename/ClassOne", "packagename/InFileClassOne", "packagename/ClassOne\$ClassOneInnerStatic", "packagename/ClassOne\$ClassOneInner", "packagename/InFileClassOne"), allClasses)

    val allClassPath = mainResolvers.flatMap { it.classPath }
    assertTrue(allClassPath.all { it.canonicalPath.replace("\\", "/").endsWith(classPath) })
  }

  private fun testCompileServerJars(classesLocations: IdePluginClassesLocations) {
    val resolver = classesLocations.getResolver(CompileServerExtensionKey)!!
    val libDirectoryClasses = resolver.allClasses
    assertSetsEqual(setOf("com/some/compile/library/CompileLibraryClass"), libDirectoryClasses)

    val compileServerJars = resolver.finalResolvers
    assertEquals(1, compileServerJars.size)
    val compileServerJar = compileServerJars[0]
    assertTrue(compileServerJar is JarFileResolver)
    val jarFileResolver = compileServerJar as JarFileResolver
    assertSetsEqual(setOf("com.example.service.Service"), jarFileResolver.implementedServiceProviders)
    val implementationNames = jarFileResolver.readServiceImplementationNames("com.example.service.Service")
    assertSetsEqual(setOf(
        "com.some.compile.library.One",
        "com.some.compile.library.Two",
        "com.some.compile.library.Three"
    ), implementationNames)
  }

  private fun <T> assertSetsEqual(expected: Set<T>, actual: Set<T>) {
    val redundant = actual - expected
    val missing = expected - actual
    val message = "Missing: [" + redundant.joinToString() + "]\nRedundant: [" + missing.joinToString() + "]"
    if (redundant.isNotEmpty() || missing.isNotEmpty()) {
      fail(message)
    }
  }

  private fun <T> assertContains(elements: Iterable<T>, shouldBe: List<T>) {
    shouldBe.filterNot { elements.contains(it) }
        .forEach { throw AssertionError("Collection must contain an element $it: [$elements]") }

  }
}
