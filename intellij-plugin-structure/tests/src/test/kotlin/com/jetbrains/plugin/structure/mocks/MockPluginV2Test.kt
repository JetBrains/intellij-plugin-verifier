package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.DirectoryFileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.classes.locator.PluginFileOrigin
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesFinder
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import com.jetbrains.plugin.structure.intellij.plugin.ModuleV2Dependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginV2Dependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginXmlUtil.getAllClassesReferencedFromXml
import com.jetbrains.plugin.structure.intellij.problems.ModuleDescriptorResolutionProblem
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Paths
import java.util.*

class MockPluginsV2Test(fileSystemType: FileSystemType) : IdePluginManagerTest(fileSystemType) {
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
    IdePluginClassesFinder.findPluginClasses(plugin, Resolver.ReadMode.FULL, emptyList()).use { classesLocations ->

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

    val moduleConfigs = modulesDescriptors.map { it.configurationFilePath }
    assertThat(
      moduleConfigs, `is`(
        listOf(
          "../intellij.v2.module-ultimate.xml", "../intellij.v2.module.xml"
        )
      )
    )

    val moduleDependencies = modulesDescriptors
      .find { it.name == "intellij.v2.module" }!!
      .dependencies

    assertThat(
      moduleDependencies.map { it.id }, `is`(
        listOf(
          "intellij.clouds.docker.remoteRun", "com.intellij.copyright"
        )
      )
    )

    assertThat(moduleDependencies.map { it.isOptional }, `is`(listOf(true, false)))

    assertTrue(plugin.dependencies.filter { it.isOptional }.map { it.id }.contains("intellij.clouds.docker.remoteRun"))
    assertTrue(plugin.dependencies.filterNot { it.isOptional }.map { it.id }.contains("com.intellij.copyright"))


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
      ModuleV2Dependency("intellij.module.dependency"),
      PluginV2Dependency("mandatoryDependencyV2"),
      PluginV2Dependency("com.intellij.modules.mandatoryDependencyV2"),
      ModuleV2Dependency("intellij.clouds.docker.remoteRun"),
      PluginV2Dependency("com.intellij.copyright"),
    )
    assertThat(plugin.dependencies, `is`(expectedDependencies))

    val expectedOptionalDependencies = listOf(
      ModuleV2Dependency("intellij.module.dependency"),
      ModuleV2Dependency("intellij.clouds.docker.remoteRun")
    )
    assertThat(plugin.dependencies.filter { it.isOptional }, `is`(expectedOptionalDependencies))
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
}