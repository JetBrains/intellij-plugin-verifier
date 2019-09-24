package com.jetbrains.plugin.structure.mocks

import com.google.common.collect.HashMultiset
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

class MockPluginsTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private val mockPluginRoot = URLUtil.urlToFile(this::class.java.getResource("/mock-plugin"))
  private val metaInfDir = mockPluginRoot.resolve("META-INF")

  private val optionalsDir = mockPluginRoot.resolve("optionalsDir")
  private val pluginClassesRoot = mockPluginRoot.resolve("classes")

  private val compileLibraryDir = mockPluginRoot.resolve("compileLibrary")
  private val compileLibraryClassesRoot = compileLibraryDir.resolve("classes")
  private val compileLibraryServices = compileLibraryDir.resolve("services")

  @Test
  fun `single jar file`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.jar")) {
      dir("META-INF", metaInfDir)
      dir("optionalsDir", optionalsDir)
      dir("somePackage", pluginClassesRoot.resolve("somePackage"))
    }
    checkPluginConfiguration(pluginFile, true) { plugin -> PluginFileOrigin.SingleJar(plugin) }
  }

  @Test
  fun `invalid jar file renamed to zip`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      zip("plugin.jar") {
        dir("META-INF", metaInfDir)
        dir("optionalsDir", optionalsDir)
        dir("somePackage", pluginClassesRoot.resolve("somePackage"))
      }
    }
    checkPluginConfiguration(pluginFile, true) { plugin -> PluginFileOrigin.SingleJar(plugin) }
  }

  @Test
  fun `plugin jar packed in lib directory of zip archive`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
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
          dir("somePackage", pluginClassesRoot.resolve("somePackage"))
        }
      }
    }

    checkPluginConfiguration(pluginFile, true) { plugin -> JarFileOrigin("plugin.jar", PluginFileOrigin.LibDirectory(plugin)) }
  }

  @Test
  fun `directory with lib subdirectory containing jar file`() {
    val pluginFile = buildDirectory(temporaryFolder.newFolder("plugin")) {
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
          dir("somePackage", pluginClassesRoot.resolve("somePackage"))
        }
      }
    }
    checkPluginConfiguration(pluginFile, true) { plugin -> JarFileOrigin("plugin.jar", PluginFileOrigin.LibDirectory(plugin)) }
  }

  @Test
  fun `plugin directory with lib containing jar file - packed in zip archive`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
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
            dir("somePackage", pluginClassesRoot.resolve("somePackage"))
          }
        }
      }
    }
    checkPluginConfiguration(pluginFile, true) { plugin -> JarFileOrigin("plugin.jar", PluginFileOrigin.LibDirectory(plugin)) }
  }

  @Test
  fun `plugin as directory with classes`() {
    val pluginFile = buildDirectory(temporaryFolder.newFolder("plugin")) {
      dir("META-INF", metaInfDir)
      dir("optionalsDir", optionalsDir)
      dir("classes") {
        dir("somePackage", pluginClassesRoot.resolve("somePackage"))
      }
      dir("lib") {
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
    checkPluginConfiguration(pluginFile, false) { plugin -> PluginFileOrigin.ClassesDirectory(plugin) }
  }

  @Test
  fun `plugin as zip with directory with classes`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      dir("plugin") {
        dir("META-INF", metaInfDir)
        dir("optionalsDir", optionalsDir)
        dir("classes") {
          dir("somePackage", pluginClassesRoot.resolve("somePackage"))
        }
        dir("lib") {
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
    checkPluginConfiguration(pluginFile, false) { plugin -> PluginFileOrigin.ClassesDirectory(plugin) }
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
      dir("somePackage", pluginClassesRoot.resolve("somePackage"))
    }

    InvalidPluginsTest.assertExpectedProblems(
        pluginFile,
        listOf(PluginZipContainsMultipleFiles(listOf("META-INF", "optionalsDir", "somePackage")))
    )
  }

  private fun checkPropertyValues(plugin: IdePlugin, resolveXIncludesWithAbsoluteHref: Boolean) {
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
    if (resolveXIncludesWithAbsoluteHref) {
      assertEquals("Change notes must be at least 40 characters long", plugin.changeNotes)
    } else {
      assertNull(plugin.changeNotes)
    }
    assertTrue(plugin.useIdeClassLoader)

    assertEquals("PABC", plugin.productDescriptor?.code)
    assertEquals(LocalDate.of(2018, 1, 18), plugin.productDescriptor?.releaseDate)
    assertEquals(20181, plugin.productDescriptor?.releaseVersion)

  }

  private fun checkWarnings(warnings: List<PluginProblem>) {
    val expectedWarnings = listOf(
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
    assertEquals(expectedWarnings.toSet(), warnings.toSet())
  }

  private fun checkIdeCompatibility(plugin: IdePlugin) {
    //  <idea-version since-build="141.1009.5" until-build="141.9999999"/>
    assertTrue(plugin.isCompatibleWithIde(IdeVersion.createIdeVersion("141.1009.5")))
    assertTrue(plugin.isCompatibleWithIde(IdeVersion.createIdeVersion("141.99999")))
    assertFalse(plugin.isCompatibleWithIde(IdeVersion.createIdeVersion("142.0")))
    assertFalse(plugin.isCompatibleWithIde(IdeVersion.createIdeVersion("141.1009.4")))
    assertFalse(plugin.isCompatibleWithIde(IdeVersion.createIdeVersion("141")))
  }

  private fun checkPluginConfiguration(
      pluginFile: File,
      resolveXIncludesWithAbsoluteHref: Boolean,
      fileOriginProvider: (IdePlugin) -> FileOrigin
  ) {
    val pluginCreationSuccess = InvalidPluginsTest.getSuccessResult(pluginFile)
    val plugin = pluginCreationSuccess.plugin
    assertEquals(pluginFile, plugin.originalFile)

    checkWarnings(pluginCreationSuccess.warnings)
    checkIdeCompatibility(plugin)
    checkPropertyValues(plugin, resolveXIncludesWithAbsoluteHref)
    checkOptionalDescriptors(plugin)
    checkExtensionPoints(plugin)
    checkDependenciesAndModules(plugin)
    checkUnderlyingDocument(plugin)

    val fileOrigin = fileOriginProvider(plugin)
    checkPluginClasses(plugin, fileOrigin)
  }

  private fun checkPluginClasses(plugin: IdePlugin, expectedFileOrigin: FileOrigin) {
    assertNotNull(plugin.originalFile)
    IdePluginClassesFinder.findPluginClasses(
        plugin,
        Resolver.ReadMode.FULL,
        listOf(CompileServerExtensionKey)
    ).use { classesLocations ->
      checkCompileServerJars(classesLocations, plugin)

      val mainResolver = CompositeResolver.create(
          IdePluginClassesFinder.MAIN_CLASSES_KEYS.flatMap { classesLocations.getResolvers(it) }
      )
      assertEquals(
          setOf("somePackage/ClassOne", "somePackage/subPackage/ClassTwo"),
          mainResolver.allClasses
      )
      assertEquals(setOf("somePackage", "somePackage/subPackage"), mainResolver.allPackages)
      assertTrue(mainResolver.containsPackage("somePackage"))
      assertTrue(mainResolver.containsPackage("somePackage/subPackage"))

      val fileOrigin = (mainResolver.resolveClass("somePackage/ClassOne") as ResolutionResult.Found).fileOrigin
      assertEquals(expectedFileOrigin, fileOrigin)
    }
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

  private fun checkExtensionPoints(plugin: IdePlugin) {
    val extensions = plugin.extensions
    val expectedExtensionPoints = HashMultiset.create(
        listOf(
            "org.intellij.scala.scalaTestDefaultWorkingDirectoryProvider",
            "com.intellij.compileServer.plugin",
            "org.jetbrains.kotlin.defaultErrorMessages",
            "org.jetbrains.kotlin.diagnosticSuppressor"
        )
    )

    assertEquals(expectedExtensionPoints, extensions.keys())
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
    assertEquals(JarFileOrigin("compile-library.jar", PluginFileOrigin.CompileServer(plugin)), fileOrigin)

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