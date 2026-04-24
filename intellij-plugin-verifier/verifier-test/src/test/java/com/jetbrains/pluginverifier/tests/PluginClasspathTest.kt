package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.BinaryClassName
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.CharSequenceComparator
import com.jetbrains.plugin.structure.base.utils.binaryClassNames
import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.classes.locator.LibModulesDirectoryLocator
import com.jetbrains.plugin.structure.intellij.classes.plugin.BundledPluginClassesFinder
import com.jetbrains.plugin.structure.intellij.classes.plugin.ClassSearchContext
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesFinder
import com.jetbrains.plugin.structure.intellij.plugin.Classpath
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.pluginverifier.createPluginResolver
import com.jetbrains.pluginverifier.resolution.BundledPluginClassResolverProvider
import com.jetbrains.pluginverifier.tests.ContentModuleFixture.content
import com.jetbrains.pluginverifier.tests.mocks.classBytes
import com.jetbrains.pluginverifier.tests.mocks.descriptor
import net.bytebuddy.ByteBuddy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginClasspathTest : BasePluginTest() {
  private val byteBuddy = ByteBuddy()

  private fun buildPluginInZip(pluginContentBuilder: ContentBuilder.() -> Unit): PluginCreationResult<IdePlugin> {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip").toPath(), pluginContentBuilder)
    val ideManager = IdePluginManager.createManager()
    return ideManager.createPlugin(pluginFile, validateDescriptor = true)
  }

  private fun buildPluginInDirectory(pluginContentBuilder: ContentBuilder.() -> Unit): PluginCreationResult<IdePlugin> {
    val pluginFile = buildDirectory(temporaryFolder.newFolder("plugin").toPath(), pluginContentBuilder)
    val ideManager = IdePluginManager.createManager()
    return ideManager.createPlugin(pluginFile, validateDescriptor = true)
  }

  @Test
  fun `classes in plugin JAR and in plugin lib-modules are discovered`() {
    val pluginId = "pluginverifier"
    val descriptor = ideaPlugin(pluginId, "Mock Classpath")
    val result = buildPluginInZip {
      dir(pluginId) {
        dir("lib") {
          zip("plugin.jar") {
            descriptor(descriptor)
            classBytes("SomeClass", byteBuddy)
          }
          dir("modules") {
            zip("plugin-module.jar") {
              classBytes("Module", byteBuddy)
            }
          }
        }
      }
    }
    assertTrue(result is PluginCreationSuccess)
    val pluginCreated = result as PluginCreationSuccess

    IdePluginClassesFinder.findPluginClasses(pluginCreated.plugin).use { classLocations ->
      classLocations.createPluginResolver().use {
        val pluginClasses = it.allClassNames
        assertEquals(2, pluginClasses.size)
        assertTrue(pluginClasses.containsAll(setOf("SomeClass", "Module")))
      }
    }
  }

  @Test
  fun `lib-modules directory locator discovers classes`() {
    val pluginId = "pluginverifier"
    val descriptor = ideaPlugin(pluginId, "Mock Classpath")
    val result = buildPluginInDirectory {
      dir("lib") {
        zip("plugin.jar") {
          descriptor(descriptor)
        }
        dir("modules") {
          zip("plugin-module.jar") {
            classBytes("Module", byteBuddy)
          }
        }
      }
    }

    assertTrue(result is PluginCreationSuccess)
    val pluginCreated = result as PluginCreationSuccess
    val plugin = pluginCreated.plugin

    val locator = LibModulesDirectoryLocator(Resolver.ReadMode.FULL)
    val classes = locator.findClasses(plugin, plugin.originalFile!!)
      .flatMapTo(binaryClassNames()) { it.allClassNames }
    assertEquals(1, classes.size)
    assertEquals(binaryClassNames("Module"), classes)
  }

  @Test
  fun `classes in lib, lib_modules are discovered`() {
    val pluginId = "pluginverifier"
    val descriptor = ideaPlugin(pluginId, "Mock Classpath")
    val result = buildPluginInDirectory {
      dir("lib") {
        zip("plugin.jar") {
          descriptor(descriptor)
          classBytes("PluginCore", byteBuddy)
        }
        dir("modules") {
          zip("plugin-module.jar") {
            classBytes("Module", byteBuddy)
          }
        }
      }
    }
    assertSuccess(result) {
      IdePluginClassesFinder.findPluginClasses(plugin).use { classLocations ->
        classLocations.createPluginResolver().use {
          val pluginClasses = it.allClassNames
          assertEquals(2, pluginClasses.size)
          assertTrue(pluginClasses.containsAll(setOf("PluginCore", "Module")))
        }
      }
    }
  }

  @Test
  fun `classes in lib, lib_modules are discovered in a bundled plugin`() {
    val pluginId = "pluginverifier"
    val descriptor = ideaPlugin(pluginId, "Mock Classpath")
    val result = buildPluginInDirectory {
      dir("lib") {
        zip("plugin.jar") {
          descriptor(descriptor)
          classBytes("PluginCore", byteBuddy)
        }
        dir("modules") {
          zip("plugin-module.jar") {
            classBytes("Module", byteBuddy)
          }
        }
      }
    }

    val classResolverProvider = BundledPluginClassResolverProvider()

    assertSuccess(result) {
      BundledPluginClassesFinder.findPluginClasses(plugin, searchContext = ClassSearchContext.DEFAULT).use { classLocations ->
        classResolverProvider.getResolver(classLocations, resolverName = pluginId).use {
          val pluginClasses = it.allClassNames
          assertEquals(2, pluginClasses.size)
          assertTrue(pluginClasses.containsAll(setOf("PluginCore", "Module")))
        }
      }
    }
  }

  @Test
  fun `lib-module and a main JAR classpath is composed with content modules in the main plugin descriptor`() {
    val pluginXml = ideaPlugin(pluginId = "com.intellij.modules.json", pluginName = "JSON") +
      content {
        module("intellij.json.split")
      }

    val result = buildPluginInDirectory {
      dir("lib") {
        dir("modules") {
          zip("intellij.json.split.jar") {
            file("intellij.json.split.xml", "<idea-plugin />")
          }
        }
        zip("json.jar") {
          descriptor(pluginXml)
          file("intellij.json.xml", "<idea-plugin />")
        }
      }
    }
    assertSuccess(result) {
      with(plugin.classpath) {
        assertEquals(2, size)
        assertTrue(containsFileName("json.jar"))
        assertTrue(containsFileName("intellij.json.split.jar"))
      }
    }
  }

  @Test
  fun `classpath is deduced from the content modules in the main plugin descriptor and actual JARs in lib slash modules directory`() {
    val pluginXml = ideaPlugin(pluginId = "com.intellij.modules.json", pluginName = "JSON") +
      content {
        module("intellij.json.frontend.split")
        embeddedModule("intellij.json")
        module("intellij.json.backend")
        module("intellij.json.backend.regexp")
        embeddedModule("intellij.json.syntax")
      }

    val result = buildPluginInDirectory {
      dir("lib") {
        dir("modules") {
          zip("intellij.json.backend.jar") {
            file("intellij.json.backend.xml", "<idea-plugin />")
          }
          zip("intellij.json.backend.regexp.jar") {
            file("intellij.json.backend.regexp.xml", "<idea-plugin />")
          }
          zip("intellij.json.frontend.split.jar") {
            file("intellij.json.frontend.split.xml", "<idea-plugin />")
          }
        }
        zip("json.jar") {
          descriptor(pluginXml)
          file("intellij.json.xml", "<idea-plugin />")
          file("intellij.json.syntax.xml", "<idea-plugin />")
        }
      }
    }
    assertSuccess(result) {
      with(plugin.classpath) {
        assertEquals(4, size)
        assertTrue(containsFileName("json.jar"))
        assertTrue(containsFileName("intellij.json.backend.jar"))
        assertTrue(containsFileName("intellij.json.frontend.split.jar"))
        assertTrue(containsFileName("intellij.json.backend.regexp.jar"))
      }
    }
  }

  @Test
  fun `lib-module does not belong to the classpath if not declared in the main plugin descriptor`() {
    val pluginXml = ideaPlugin(pluginId = "com.intellij.modules.json", pluginName = "JSON")

    val result = buildPluginInDirectory {
      dir("lib") {
        dir("modules") {
          zip("intellij.json.split.jar") {
            file("intellij.json.split.xml", "<idea-plugin />")
          }
        }
        zip("json.jar") {
          descriptor(pluginXml)
          file("intellij.json.xml", "<idea-plugin />")
        }
      }
    }
    assertSuccess(result) {
      with(plugin.classpath) {
        assertEquals(1, size)
        assertTrue(containsFileName("json.jar"))
      }
    }
  }

  fun Classpath.containsFileName(fileName: String): Boolean {
    return entries.any { it.path.fileName.toString() == fileName }
  }

  private fun assertEquals(expected: Set<BinaryClassName>, actual: Set<BinaryClassName>): Boolean {
    if (expected == actual) return true
    if (expected.size != actual.size) return false
    for (expectedClass in expected) {
      for (actualClass in actual) {
        if (CharSequenceComparator.compare(expectedClass, actualClass) != 0) {
          return false
        }
      }
    }
    return true
  }
}