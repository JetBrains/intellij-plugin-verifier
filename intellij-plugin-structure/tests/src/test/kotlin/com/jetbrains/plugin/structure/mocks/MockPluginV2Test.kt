package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.classes.resolvers.*
import com.jetbrains.plugin.structure.intellij.classes.locator.CompileServerExtensionKey
import com.jetbrains.plugin.structure.intellij.classes.locator.PluginFileOrigin
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesFinder
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.*
import com.jetbrains.plugin.structure.intellij.plugin.PluginXmlUtil.getAllClassesReferencedFromXml
import com.jetbrains.plugin.structure.intellij.problems.ModuleDescriptorResolutionProblem
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

class MockPluginsV2Test(fileSystemType: FileSystemType) : BasePluginManagerTest<IdePlugin, IdePluginManager>(fileSystemType) {
  private val mockPluginRoot = Paths.get(this::class.java.getResource("/mock-plugin-v2").toURI())
  private val metaInfDir = mockPluginRoot.resolve("META-INF")
  private val v2ModuleFile = mockPluginRoot.resolve("intellij.v2.module.xml")
  private val v2ModuleFileUltimate = mockPluginRoot.resolve("intellij.v2.module-ultimate.xml")
  private val xiIncludeDir = mockPluginRoot.resolve("xiIncludeDir")

  private val propertiesDir = mockPluginRoot.resolve("properties")
  private val somePackageDir = mockPluginRoot.resolve("classes").resolve("somePackage")

  private val expectedWarnings = listOf(
    ModuleDescriptorResolutionProblem(
      "intellij.v2.missing",
      "../intellij.v2.missing.xml",
      listOf(PluginDescriptorIsNotFound("../intellij.v2.missing.xml"))
    )
  )

  private fun buildPluginSuccess(expectedWarnings: List<PluginProblem>, pluginFileBuilder: () -> Path): IdePlugin {
    val pluginFile = pluginFileBuilder()
    val successResult = createPluginSuccessfully(pluginFile)
    val (plugin, warnings) = successResult
    assertEquals(expectedWarnings.toSet().sortedBy { it.message }, warnings.toSet().sortedBy { it.message })
    assertEquals(pluginFile, plugin.originalFile)
    return plugin
  }

  override fun createManager(extractDirectory: Path): IdePluginManager =
    IdePluginManager.createManager(extractDirectory)

  @Test
  fun `single jar file`() {
    val plugin = buildPluginSuccess(expectedWarnings) {
      buildZipFile(temporaryFolder.newFile("plugin.jar")) {
        file("intellij.v2.module.xml", v2ModuleFile)
        file("intellij.v2.module-ultimate.xml", v2ModuleFileUltimate)
        dir("META-INF", metaInfDir)
        dir("xiIncludeDir", xiIncludeDir)
        dir("somePackage", somePackageDir)
        dir("properties", propertiesDir)
      }
    }

    checkPluginConfiguration(plugin, false)

    val jarOrigin = PluginFileOrigin.SingleJar(plugin)
    checkPluginClassesAndProperties(plugin, jarOrigin, jarOrigin)
  }

  @Test
  fun `classes and resources directories inside lib`() {
    val plugin = buildPluginSuccess(expectedWarnings) {
      buildDirectory(temporaryFolder.newFolder("plugin")) {
        file("intellij.v2.module.xml", v2ModuleFile)
        file("intellij.v2.module-ultimate.xml", v2ModuleFileUltimate)
        dir("META-INF", metaInfDir)
        dir("xiIncludeDir", xiIncludeDir)
        dir("lib") {
          dir("classes") {
            dir("somePackage", somePackageDir)
          }

          dir("resources") {
            dir("properties", propertiesDir)
          }
        }
      }
    }
    checkPluginConfiguration(plugin, true)

    val libDirectory = PluginFileOrigin.LibDirectory(plugin)
    val classesDirOrigin = DirectoryFileOrigin("classes", libDirectory)
    val resourcesJarOrigin = DirectoryFileOrigin("resources", libDirectory)
    checkPluginClassesAndProperties(plugin, classesDirOrigin, resourcesJarOrigin)
  }

  private fun checkPluginValues(plugin: IdePlugin, isDirectoryBasedPlugin: Boolean) {
    assertEquals("https://github.com/JetBrains/intellij-plugins/tree/master/vuejs", plugin.url)
    assertEquals("VueJS", plugin.pluginName)
    assertEquals("231.7515.9", plugin.pluginVersion)
    assertEquals("org.jetbrains.vuejs2", plugin.pluginId)

    assertEquals("vendor_email", plugin.vendorEmail)
    assertEquals("https://www.jetbrains.com", plugin.vendorUrl)
    assertEquals("JetBrains s.r.o.", plugin.vendor)

    assertEquals("Plugin description must be at least 40 characters long", plugin.description)
    assertEquals(IdeVersion.createIdeVersion("141.1009.5"), plugin.sinceBuild)
    assertEquals(IdeVersion.createIdeVersion("141.9999999"), plugin.untilBuild)

    assertNull(plugin.changeNotes)
    assertFalse(plugin.useIdeClassLoader)
    assertFalse(plugin.isImplementationDetail)
    assertTrue(plugin.isV2)
  }

  private fun checkIdeCompatibility(plugin: IdePlugin) {
    //  <idea-version since-build="141.1009.5" until-build="141.9999999"/>
    assertTrue(plugin.isCompatibleWithIde(IdeVersion.createIdeVersion("141.1009.5")))
    assertTrue(plugin.isCompatibleWithIde(IdeVersion.createIdeVersion("141.99999")))
    assertFalse(plugin.isCompatibleWithIde(IdeVersion.createIdeVersion("142.0")))
    assertFalse(plugin.isCompatibleWithIde(IdeVersion.createIdeVersion("141.1009.4")))
    assertFalse(plugin.isCompatibleWithIde(IdeVersion.createIdeVersion("141")))
  }

  private fun checkPluginClassesAndProperties(
    plugin: IdePlugin,
    classFilesOrigin: FileOrigin,
    propertyFileOrigin: FileOrigin
  ) {
    assertNotNull(plugin.originalFile)
    IdePluginClassesFinder.findPluginClasses(plugin, Resolver.ReadMode.FULL, listOf(CompileServerExtensionKey)).use { classesLocations ->
      checkCompileServerJars(classesLocations, plugin)

      val mainResolver = CompositeResolver.create(
        IdePluginClassesFinder.MAIN_CLASSES_KEYS.flatMap { classesLocations.getResolvers(it) }
      )

      checkPluginClasses(mainResolver, classFilesOrigin)
      checkPluginProperties(mainResolver, propertyFileOrigin)
    }
  }

  private fun checkPluginConfiguration(plugin: IdePlugin, isDirectoryBasedPlugin: Boolean) {
    checkPluginValues(plugin, isDirectoryBasedPlugin)
    checkIdeCompatibility(plugin)
    checkModulesDescriptors(plugin)
    checkPluginContents(plugin as IdePluginImpl)
    checkDependenciesAndModules(plugin)
    checkUnderlyingDocument(plugin)
  }

  private fun checkPluginClasses(resolver: Resolver, expectedFileOrigin: FileOrigin) {
    assertEquals(
      setOf("somePackage/ClassOne", "somePackage/subPackage/ClassTwo"),
      resolver.allClasses
    )
    assertEquals(setOf("somePackage", "somePackage/subPackage"), resolver.allPackages)
    assertTrue(resolver.containsPackage("somePackage"))
    assertTrue(resolver.containsPackage("somePackage/subPackage"))

    val fileOrigin = (resolver.resolveClass("somePackage/ClassOne") as ResolutionResult.Found).fileOrigin
    assertEquals(expectedFileOrigin, fileOrigin)
  }

  private fun checkPluginProperties(resolver: Resolver, propertyFileOrigin: FileOrigin) {
    val bundleNameSet = resolver.allBundleNameSet
    assertEquals(setOf("properties.someBundle"), bundleNameSet.baseBundleNames)
    assertEquals(setOf("properties.someBundle", "properties.someBundle_en"), bundleNameSet["properties.someBundle"])

    val (enBundle, enFileOrigin) = resolver.resolveExactPropertyResourceBundle("properties.someBundle", Locale.ENGLISH) as ResolutionResult.Found

    assertEquals(propertyFileOrigin, enFileOrigin)
    assertEquals("en.only.property.value", enBundle.getString("en.only.property"))
    assertEquals("new.overridden.key.value", enBundle.getString("overridden.key"))
    assertNull(enBundle.handleGetObject("common.key"))

    val (rootBundle, rootFileOrigin) = resolver.resolveExactPropertyResourceBundle("properties.someBundle", Locale.ROOT) as ResolutionResult.Found

    assertEquals(propertyFileOrigin, rootFileOrigin)
    assertEquals("common.key.value", rootBundle.getString("common.key"))
    assertEquals("overridden.key.value", rootBundle.getString("overridden.key"))
  }

  private fun checkUnderlyingDocument(plugin: IdePlugin) {
    val document = plugin.underlyingDocument
    val rootElement = document.rootElement
    assertNotNull(rootElement)
    assertEquals("idea-plugin", rootElement.name)
  }

  private fun checkModulesDescriptors(plugin: IdePlugin) {
    val modulesDescriptors = plugin.modulesDescriptors
    assertEquals(
      listOf(
        "../intellij.v2.module-ultimate.xml",
        "../intellij.v2.module.xml"
      ),
      modulesDescriptors.map { it.configurationFilePath }
    )

    val moduleDependencies = modulesDescriptors.find { it.name == "intellij.v2.module" }!!.dependencies
    assertEquals(
      listOf(
        "intellij.clouds.docker.remoteRun",
        "com.intellij.copyright"
      ),
      moduleDependencies.map { it.id }
    )

    assertEquals(
      listOf(false, false),
      moduleDependencies.map { it.isOptional }
    )

    assertTrue(plugin.dependencies.filter { it.isOptional }.map { it.id }.contains("intellij.clouds.docker.remoteRun"))
    assertTrue(plugin.dependencies.filter { it.isOptional }.map { it.id }.contains("com.intellij.copyright"))


    assertEquals(
      setOf("com.test.class".replace('.', '/'), "com.intellij.BeanClassUltimate".replace('.', '/')),
      plugin.modulesDescriptors.flatMap { getAllClassesReferencedFromXml(it.module) }.toSet()
    )

    assertEquals(
      setOf("com.test.class".replace('.', '/')),
      getAllClassesReferencedFromXml(modulesDescriptors.find { it.name == "intellij.v2.module" }!!.module)
    )

    assertEquals(
      setOf("com.intellij.BeanClassUltimate".replace('.', '/')),
      getAllClassesReferencedFromXml(modulesDescriptors.find { it.name == "intellij.v2.module-ultimate" }!!.module)
    )
  }

  private fun checkDependenciesAndModules(plugin: IdePlugin) {
    assertEquals(5, plugin.dependencies.size.toLong())
    //check plugin and module dependencies
    val expectedDependencies = listOf(
      PluginDependencyImpl("intellij.module.dependency", false, false),
      PluginDependencyImpl("mandatoryDependencyV2", false, false),
      PluginDependencyImpl("com.intellij.modules.mandatoryDependencyV2", false, true),
      PluginDependencyImpl("intellij.clouds.docker.remoteRun", true, false),
      PluginDependencyImpl("com.intellij.copyright", true, false),
    )
    assertEquals(expectedDependencies, plugin.dependencies)
  }

  private fun checkPluginContents(plugin: IdePluginImpl) {
    assertEquals(
      setOf(
        "EpWithDefaultNs.someEP",
        "com.intellij.copyright.updater"
      ),
      plugin.extensions.keys.toSet()
    )

    val appContainerDescriptor = plugin.appContainerDescriptor
    val projectContainerDescriptor = plugin.projectContainerDescriptor
    val moduleContainerDescriptor = plugin.moduleContainerDescriptor

    assertEquals(
      setOf(
        "org.jetbrains.vuejs2.appEP",
        "org.jetbrains.vuejs2.appEP2",
        "org.jetbrains.vuejs2.optionalUpdaterV2Ultimate"
      ),
      appContainerDescriptor.extensionPoints.map { it.extensionPointName }.toSet()
    )

    assertEquals(
      setOf("org.jetbrains.vuejs2.projectEP"),
      projectContainerDescriptor.extensionPoints.map { it.extensionPointName }.toSet()
    )

    assertEquals(
      setOf("org.jetbrains.vuejs2.moduleEP"),
      moduleContainerDescriptor.extensionPoints.map { it.extensionPointName }.toSet()
    )

    assertEquals(2, plugin.actions.size)
    assertNotNull(plugin.actions.find { it.getAttributeValue("class") == "SomeActionClass" })
    assertNotNull(plugin.actions.find { it.getAttributeValue("id") == "SomeGroupId" })

    val module = plugin.modulesDescriptors.find { it.name == "intellij.v2.module-ultimate" }!!.module as IdePluginImpl
    assertEquals(
      setOf("org.jetbrains.vuejs2.optionalUpdaterV2Ultimate"),
      module.appContainerDescriptor.extensionPoints.map { it.extensionPointName }.toSet()
    )
  }

  private fun checkCompileServerJars(classesLocations: IdePluginClassesLocations, plugin: IdePlugin) {
    val resolvers = classesLocations.getResolvers(CompileServerExtensionKey)
    if (resolvers.isEmpty()) {
      return
    }
    val singleResolver = resolvers.single() as JarFileResolver

    val libDirectoryClasses = singleResolver.allClasses

    val compileLibraryClass = "com/some/compile/library/CompileLibraryClass"
    assertEquals(setOf(compileLibraryClass), libDirectoryClasses)

    val fileOrigin = (singleResolver.resolveClass(compileLibraryClass) as ResolutionResult.Found).fileOrigin
    assertEquals(JarOrZipFileOrigin("compile-library.jar", PluginFileOrigin.CompileServer(plugin)), fileOrigin)

    assertEquals(
      mapOf(
        "com.example.service.Service" to setOf(
          "com.some.compile.library.One",
          "com.some.compile.library.Two",
          "com.some.compile.library.Three"
        )
      ),
      singleResolver.implementedServiceProviders
    )
  }

}