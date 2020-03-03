package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.base.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.base.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.MultiplePluginDescriptors
import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound
import com.jetbrains.plugin.structure.classes.resolvers.*
import com.jetbrains.plugin.structure.intellij.classes.locator.CompileServerExtensionKey
import com.jetbrains.plugin.structure.intellij.classes.locator.PluginFileOrigin
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesFinder
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.plugin.PluginXmlUtil.getAllClassesReferencedFromXml
import com.jetbrains.plugin.structure.intellij.problems.DuplicatedDependencyWarning
import com.jetbrains.plugin.structure.intellij.problems.OptionalDependencyDescriptorResolutionProblem
import com.jetbrains.plugin.structure.intellij.problems.PluginZipContainsMultipleFiles
import com.jetbrains.plugin.structure.intellij.utils.URLUtil
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate
import java.util.*

class MockPluginsTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private val mockPluginRoot = URLUtil.urlToFile(this::class.java.getResource("/mock-plugin"))
  private val metaInfDir = mockPluginRoot.resolve("META-INF")

  private val optionalsDir = mockPluginRoot.resolve("optionalsDir")
  private val propertiesDir = mockPluginRoot.resolve("properties")
  private val somePackageDir = mockPluginRoot.resolve("classes").resolve("somePackage")

  private val compileLibraryDir = mockPluginRoot.resolve("compileLibrary")
  private val compileLibraryClassesRoot = compileLibraryDir.resolve("classes")
  private val compileLibraryServices = compileLibraryDir.resolve("services")

  private val expectedWarnings = listOf(
    OptionalDependencyDescriptorResolutionProblem(
      "missingDependency",
      "missingFile.xml",
      listOf(PluginDescriptorIsNotFound("missingFile.xml"))
    ),
    OptionalDependencyDescriptorResolutionProblem(
      "referenceFromRoot",
      "/META-INF/referencedFromRoot.xml",
      listOf(PluginDescriptorIsNotFound("/META-INF/referencedFromRoot.xml"))
    ),
    DuplicatedDependencyWarning("duplicatedDependencyId")
  )

  private fun buildPluginSuccess(expectedWarnings: List<PluginProblem>, pluginFileBuilder: () -> File): IdePlugin {
    val pluginFile = pluginFileBuilder()
    val successResult = InvalidPluginsTest.getSuccessResult(pluginFile)
    val (plugin, warnings) = successResult
    assertEquals(expectedWarnings.toSet().sortedBy { it.message }, warnings.toSet().sortedBy { it.message })
    assertEquals(pluginFile, plugin.originalFile)
    return plugin
  }

  @Test
  fun `single jar file`() {
    val plugin = buildPluginSuccess(expectedWarnings) {
      buildZipFile(temporaryFolder.newFile("plugin.jar")) {
        dir("META-INF", metaInfDir)
        dir("optionalsDir", optionalsDir)
        dir("somePackage", somePackageDir)
        dir("properties", propertiesDir)
      }
    }

    checkPluginConfiguration(plugin, false)

    val jarOrigin = PluginFileOrigin.SingleJar(plugin)
    checkPluginClassesAndProperties(plugin, jarOrigin, jarOrigin)
  }

  @Test
  fun `invalid jar file renamed to zip`() {
    val plugin = buildPluginSuccess(expectedWarnings) {
      buildZipFile(temporaryFolder.newFile("plugin.zip")) {
        zip("plugin.jar") {
          dir("META-INF", metaInfDir)
          dir("optionalsDir", optionalsDir)
          dir("somePackage", somePackageDir)
          dir("properties", propertiesDir)
        }
      }
    }

    checkPluginConfiguration(plugin, false)

    val jarOrigin = PluginFileOrigin.SingleJar(plugin)
    checkPluginClassesAndProperties(plugin, jarOrigin, jarOrigin)
  }

  @Test
  fun `plugin jar packed in lib directory of zip archive`() {
    val plugin = buildPluginSuccess(expectedWarnings) {
      buildZipFile(temporaryFolder.newFile("plugin.zip")) {
        dir("lib") {
          dir("compile") {
            zip("compile-library.jar") {
              dir("META-INF") {
                dir("services", compileLibraryServices)
              }
              dir("com", compileLibraryClassesRoot.resolve("com"))
            }
          }

          zip("plugin.jar") {
            dir("META-INF", metaInfDir)
            dir("optionalsDir", optionalsDir)
            dir("somePackage", somePackageDir)
            dir("properties", propertiesDir)
          }
        }
      }
    }

    checkPluginConfiguration(plugin, false)

    val pluginJarOrigin = JarOrZipFileOrigin("plugin.jar", PluginFileOrigin.LibDirectory(plugin))
    checkPluginClassesAndProperties(plugin, pluginJarOrigin, pluginJarOrigin)
  }

  @Test
  fun `directory with lib subdirectory containing jar file`() {
    val plugin = buildPluginSuccess(expectedWarnings) {
      buildDirectory(temporaryFolder.newFolder("plugin")) {
        dir("lib") {
          dir("compile") {
            zip("compile-library.jar") {
              dir("META-INF") {
                dir("services", compileLibraryServices)
              }
              dir("com", compileLibraryClassesRoot.resolve("com"))
            }
          }

          zip("plugin.jar") {
            dir("META-INF", metaInfDir)
            dir("optionalsDir", optionalsDir)
            dir("somePackage", somePackageDir)
            dir("properties", propertiesDir)
          }
        }
      }
    }
    checkPluginConfiguration(plugin, false)

    val pluginJarOrigin = JarOrZipFileOrigin("plugin.jar", PluginFileOrigin.LibDirectory(plugin))
    checkPluginClassesAndProperties(plugin, pluginJarOrigin, pluginJarOrigin)
  }

  @Test
  fun `plugin directory with lib containing jar file - packed in zip archive`() {
    val plugin = buildPluginSuccess(expectedWarnings) {
      buildZipFile(temporaryFolder.newFile("plugin.zip")) {
        dir("plugin") {
          dir("lib") {
            dir("compile") {
              zip("compile-library.jar") {
                dir("META-INF") {
                  dir("services", compileLibraryServices)
                }
                dir("com", compileLibraryClassesRoot.resolve("com"))
              }
            }

            zip("plugin.jar") {
              dir("META-INF", metaInfDir)
              dir("optionalsDir", optionalsDir)
              dir("somePackage", somePackageDir)
              dir("properties", propertiesDir)
            }
          }
        }
      }
    }
    checkPluginConfiguration(plugin, false)

    val pluginJarOrigin = JarOrZipFileOrigin("plugin.jar", PluginFileOrigin.LibDirectory(plugin))
    checkPluginClassesAndProperties(plugin, pluginJarOrigin, pluginJarOrigin)
  }

  @Test
  fun `plugin as directory with classes`() {
    val plugin = buildPluginSuccess(expectedWarnings) {
      buildDirectory(temporaryFolder.newFolder("plugin")) {
        dir("META-INF", metaInfDir)
        dir("optionalsDir", optionalsDir)
        dir("classes") {
          dir("somePackage", somePackageDir)
        }
        dir("lib") {
          zip("resources.jar") {
            dir("properties", propertiesDir)
          }

          dir("compile") {
            zip("compile-library.jar") {
              dir("META-INF") {
                dir("services", compileLibraryServices)
              }
              dir("com", compileLibraryClassesRoot.resolve("com"))
            }
          }
        }
      }
    }
    checkPluginConfiguration(plugin, true)

    val classesDirOrigin = PluginFileOrigin.ClassesDirectory(plugin)
    val resourcesJarOrigin = JarOrZipFileOrigin("resources.jar", PluginFileOrigin.LibDirectory(plugin))
    checkPluginClassesAndProperties(plugin, classesDirOrigin, resourcesJarOrigin)
  }

  @Test
  fun `classes and resources directories inside lib`() {
    val plugin = buildPluginSuccess(expectedWarnings) {
      buildDirectory(temporaryFolder.newFolder("plugin")) {
        dir("META-INF", metaInfDir)
        dir("optionalsDir", optionalsDir)
        dir("lib") {
          dir("classes") {
            dir("somePackage", somePackageDir)
          }

          dir("resources") {
            dir("properties", propertiesDir)
          }

          dir("compile") {
            zip("compile-library.jar") {
              dir("META-INF") {
                dir("services", compileLibraryServices)
              }
              dir("com", compileLibraryClassesRoot.resolve("com"))
            }
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

  @Test
  fun `plugin as zip with directory with classes`() {
    val plugin = buildPluginSuccess(expectedWarnings) {
      buildZipFile(temporaryFolder.newFile("plugin.zip")) {
        dir("plugin") {
          dir("META-INF", metaInfDir)
          dir("optionalsDir", optionalsDir)
          dir("classes") {
            dir("somePackage", somePackageDir)
          }
          dir("lib") {
            zip("resources.jar") {
              dir("properties", propertiesDir)
            }

            dir("compile") {
              zip("compile-library.jar") {
                dir("META-INF") {
                  dir("services", compileLibraryServices)
                }
                dir("com", compileLibraryClassesRoot.resolve("com"))
              }
            }
          }
        }
      }
    }

    checkPluginConfiguration(plugin, true)

    val classesDirOrigin = PluginFileOrigin.ClassesDirectory(plugin)
    val resourcesJarOrigin = JarOrZipFileOrigin("resources.jar", PluginFileOrigin.LibDirectory(plugin))
    checkPluginClassesAndProperties(plugin, classesDirOrigin, resourcesJarOrigin)
  }

  @Test
  fun `transitive dependencies in optional dependency configuration file`() {
    val plugin = buildPluginSuccess(emptyList()) {
      buildZipFile(temporaryFolder.newFile("plugin.jar")) {
        dir("META-INF") {
          file("plugin.xml") {
            perfectXmlBuilder.modify {
              depends += """<depends optional="true" config-file="optionalDependency.xml">optionalDependencyId</depends>"""
            }
          }

          file(
            "optionalDependency.xml",
            """
                <idea-plugin>
                  <depends>transitiveMandatoryDependencyId</depends>
                  <depends optional="true" config-file="transitiveOptionalDependency.xml">transitiveOptionalDependencyId</depends>
                </idea-plugin>
              """.trimIndent()
          )

          file("transitiveOptionalDependency.xml", """<idea-plugin></idea-plugin>""".trimIndent())
        }
      }
    }

    val optionalDescriptor = plugin.optionalDescriptors.single()
    assertEquals("optionalDependency.xml", optionalDescriptor.configurationFilePath)
    assertEquals(PluginDependencyImpl("optionalDependencyId", true, false), optionalDescriptor.dependency)

    val optionalPlugin = optionalDescriptor.optionalPlugin
    assertEquals(listOf("transitiveMandatoryDependencyId", "transitiveOptionalDependencyId"), optionalPlugin.dependencies.map { it.id })

    val transitiveOptional = optionalPlugin.optionalDescriptors.find { it.dependency.id == "transitiveOptionalDependencyId" }!!
    assertEquals("transitiveOptionalDependency.xml", transitiveOptional.configurationFilePath)
    assertEquals(PluginDependencyImpl("transitiveOptionalDependencyId", true, false), transitiveOptional.dependency)
  }

  @Test
  fun `plugin has optional dependency where configuration file is ambiguous in two jar files`() {
    /*
      plugin/
        lib/
          plugin.jar!/
            META-INF/
              plugin.xml (references <depends optional="true" config-file="optionalDependency.xml">someId</depends>)
          one.jar!/
            META-INF/
              optionalDependency.xml  <--- duplicated
          two.jar!/
            META-INF/
              optionalDependency.xml  <--- duplicated
    */
    val pluginDirectory = buildDirectory(temporaryFolder.newFolder("plugin")) {
      dir("lib") {
        zip("plugin.jar") {
          dir("META-INF") {
            file("plugin.xml") {
              perfectXmlBuilder.modify {
                depends += """<depends optional="true" config-file="optionalDependency.xml">someDependencyId</depends>"""
              }
            }
          }
        }

        zip("one.jar") {
          dir("META-INF") {
            file("optionalDependency.xml", "<idea-plugin></idea-plugin>")
          }
        }

        zip("two.jar") {
          dir("META-INF") {
            file("optionalDependency.xml", "<idea-plugin></idea-plugin>")
          }
        }
      }
    }

    val creationSuccess = InvalidPluginsTest.getSuccessResult(pluginDirectory)
    assertEquals(
      creationSuccess.warnings,
      listOf(
        OptionalDependencyDescriptorResolutionProblem(
          "someDependencyId",
          "optionalDependency.xml",
          listOf(MultiplePluginDescriptors("optionalDependency.xml", "one.jar", "optionalDependency.xml", "two.jar"))
        )
      )
    )
  }

  @Test
  fun `invalid plugin with classes packed in a root of a zip`() {
    //Plugin .jar that was renamed to .zip is not allowed.

    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      dir("META-INF", metaInfDir)
      dir("optionalsDir", optionalsDir)
      dir("somePackage", somePackageDir)
      dir("properties", propertiesDir)
    }

    InvalidPluginsTest.assertExpectedProblems(
      pluginFile,
      listOf(PluginZipContainsMultipleFiles(listOf("META-INF", "optionalsDir", "properties", "somePackage")))
    )
  }

  private fun checkPluginValues(plugin: IdePlugin, isDirectoryBasedPlugin: Boolean) {
    assertEquals("https://kotlinlang.org", plugin.url)
    assertEquals("Kotlin", plugin.pluginName)
    assertEquals("1.0.0-beta-1038-IJ141-17", plugin.pluginVersion)
    assertEquals("org.jetbrains.kotlin2", plugin.pluginId)

    assertEquals("vendor_email", plugin.vendorEmail)
    assertEquals("https://www.jetbrains.com", plugin.vendorUrl)
    assertEquals("JetBrains s.r.o.", plugin.vendor)

    assertEquals("Plugin description must be at least 40 characters long", plugin.description)
    assertEquals(IdeVersion.createIdeVersion("141.1009.5"), plugin.sinceBuild)
    assertEquals(IdeVersion.createIdeVersion("141.9999999"), plugin.untilBuild)

    /*
    <change-notes> element will be included only if the plugin is NOT directory-based.

    It resides in "change-notes.xml" file, which is referenced in test data "plugin.xml" as
      <xi:include href="/META-INF/change-notes.xml" xpointer="xpointer(/idea-plugin/)">
    via absolute path "/META-INF/change-notes.xml".

    For directory-based plugins, like "<plugin-directory>/META-INF/plugin.xml" absolute paths
    are not resolved against <plugin-directory> but against "/" root of the file system so they can't be resolved
    and the corresponding plugin must be considered invalid.
     */
    if (!isDirectoryBasedPlugin) {
      assertEquals("Change notes must be at least 40 characters long", plugin.changeNotes)
    } else {
      assertNull(plugin.changeNotes)
    }
    assertTrue(plugin.useIdeClassLoader)

    assertEquals("PABC", plugin.productDescriptor?.code)
    assertEquals(LocalDate.of(2018, 1, 18), plugin.productDescriptor?.releaseDate)
    assertEquals(20181, plugin.productDescriptor?.releaseVersion)

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
    checkOptionalDescriptors(plugin)
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

  private fun checkOptionalDescriptors(plugin: IdePlugin) {
    val optionalDescriptors = plugin.optionalDescriptors
    assertEquals(
      listOf("extension.xml", "optionals/optional.xml", "../optionalsDir/otherDirOptional.xml"),
      optionalDescriptors.map { it.configurationFilePath }
    )

    assertEquals(
      listOf("JUnit", "optionalDependency", "otherDirOptionalDependency"),
      optionalDescriptors.map { it.dependency.id }
    )

    assertEquals(
      listOf(true, true, true),
      optionalDescriptors.map { it.dependency.isOptional }
    )

    assertEquals(
      setOf("org.jetbrains.plugins.scala.project.maven.MavenWorkingDirectoryProviderImpl".replace('.', '/')),
      getAllClassesReferencedFromXml(optionalDescriptors.find { it.dependency.id == "JUnit" }!!.optionalPlugin)
    )

    assertEquals(
      setOf("com.intellij.BeanClass".replace('.', '/')),
      getAllClassesReferencedFromXml(optionalDescriptors.find { it.dependency.id == "optionalDependency" }!!.optionalPlugin)
    )

    assertEquals(
      setOf("com.intellij.optional.BeanClass".replace('.', '/')),
      getAllClassesReferencedFromXml(optionalDescriptors.find { it.dependency.id == "otherDirOptionalDependency" }!!.optionalPlugin)
    )
  }

  private fun checkDependenciesAndModules(plugin: IdePlugin) {
    assertEquals(9, plugin.dependencies.size.toLong())
    //check plugin and module dependencies
    val expectedDependencies = listOf(
      PluginDependencyImpl("JUnit", true, false),
      PluginDependencyImpl("optionalDependency", true, false),
      PluginDependencyImpl("otherDirOptionalDependency", true, false),
      PluginDependencyImpl("referenceFromRoot", true, false),
      PluginDependencyImpl("missingDependency", true, false),
      PluginDependencyImpl("mandatoryDependency", false, false),
      PluginDependencyImpl("com.intellij.modules.mandatoryDependency", false, true),
      PluginDependencyImpl("duplicatedDependencyId", false, false),
      PluginDependencyImpl("duplicatedDependencyId", false, false)
    )
    assertEquals(expectedDependencies, plugin.dependencies)

    assertEquals(setOf("one_module"), plugin.definedModules)
  }

  private fun checkPluginContents(plugin: IdePluginImpl) {
    assertEquals(
        setOf(
        "org.intellij.scala.scalaTestDefaultWorkingDirectoryProvider",
        "com.intellij.compileServer.plugin",
        "EpWithDefaultNs.someEP"
      ),
        plugin.extensions.keys.toSet()
    )

    val appContainerDescriptor = plugin.appContainerDescriptor
    val projectContainerDescriptor = plugin.projectContainerDescriptor
    val moduleContainerDescriptor = plugin.moduleContainerDescriptor

    assertEquals(
      setOf(
        "org.jetbrains.kotlin2.updater",
        "org.jetbrains.kotlin2.appEP",
        "org.jetbrains.kotlin2.appEP2",
        "org.jetbrains.kotlin2.optionalUpdater"
      ),
      appContainerDescriptor.extensionPoints.map { it.extensionPointName }.toSet()
    )
    assertTrue(appContainerDescriptor.extensionPoints.find { it.extensionPointName == "org.jetbrains.kotlin2.optionalUpdater" }!!.isDynamic)

    assertEquals(
      setOf("SomeApplicationComponent"),
      appContainerDescriptor.components.map { it.implementationClass }.toSet()
    )

    assertEquals(
      setOf("org.jetbrains.kotlin2.projectEP"),
      projectContainerDescriptor.extensionPoints.map { it.extensionPointName }.toSet()
    )

    assertEquals(
      setOf("SomeProjectComponent"),
      projectContainerDescriptor.components.map { it.implementationClass }.toSet()
    )

    assertEquals(
      setOf("org.jetbrains.kotlin2.moduleEP"),
      moduleContainerDescriptor.extensionPoints.map { it.extensionPointName }.toSet()
    )

    assertEquals(
      setOf("SomeModuleComponent"),
      moduleContainerDescriptor.components.map { it.implementationClass }.toSet()
    )

    assertEquals(2, plugin.actions.size)
    assertNotNull(plugin.actions.find { it.getAttributeValue("class") == "SomeActionClass" })
    assertNotNull(plugin.actions.find { it.getAttributeValue("id") == "SomeGroupId" })

    val optionalPlugin = plugin.optionalDescriptors.find { it.dependency.id == "optionalDependency" }!!.optionalPlugin as IdePluginImpl
    assertEquals(
      setOf("org.jetbrains.kotlin2.optionalUpdater"),
      optionalPlugin.appContainerDescriptor.extensionPoints.map { it.extensionPointName }.toSet()
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