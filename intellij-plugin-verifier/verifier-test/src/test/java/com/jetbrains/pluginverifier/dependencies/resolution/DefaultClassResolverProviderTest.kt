package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.repository.files.IdleFileLock
import com.jetbrains.pluginverifier.resolution.DefaultClassResolverProvider
import com.jetbrains.pluginverifier.tests.BaseBytecodeTest
import com.jetbrains.pluginverifier.tests.mocks.MockDependencyFinder
import com.jetbrains.pluginverifier.tests.mocks.MockIdePlugin
import com.jetbrains.pluginverifier.tests.mocks.MockPackageFilter
import com.jetbrains.pluginverifier.tests.mocks.createMockPluginInfo
import org.intellij.lang.annotations.Language
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.Closeable

class DefaultClassResolverProviderTest : BaseBytecodeTest() {
  private val dependencyFinder = MockDependencyFinder()

  private val packageFilter = MockPackageFilter()

  private val plugin = MockIdePlugin(
    pluginId = "somePlugin",
    pluginVersion = "1.0"
  )

  @Test
  fun `legacy plugin without any dependencies resolves to IDEA Core plugin in Platform 192`() {
    val ide = buildIdeWithBundledPlugins()
    val ideDescriptor = IdeDescriptor.create(ide.idePath, defaultJdkPath = null, ideFileLock = null)

    val resolverProvider = DefaultClassResolverProvider(dependencyFinder, ideDescriptor, packageFilter)

    val classResolver = resolverProvider.provide(plugin.details)
    // class from app.jar from mock IDE
    assertTrue(classResolver.allResolver.containsClass("com/intellij/tasks/Task"))
  }

  @Test
  fun `legacy plugin without any dependencies resolves to IDEA Core plugin in Platform 243`() {
    val ide = buildIdeWithBundledPlugins(version = "IU-243.21565.193", productInfo = productInfoJsonIU243, hasModuleDescriptors = true)
    val ideDescriptor = IdeDescriptor.create(ide.idePath, defaultJdkPath = null, ideFileLock = null)

    val resolverProvider = DefaultClassResolverProvider(dependencyFinder, ideDescriptor, packageFilter)

    val classResolver = resolverProvider.provide(plugin.details)
    // class from app.jar from mock IDE
    assertTrue(classResolver.allResolver.containsClass("com/intellij/tasks/Task"))
  }

  private val IdePlugin.emptyClassesLocations
    get() = IdePluginClassesLocations(this, allocatedResource = Closeable {}, locations = emptyMap())

  private val IdePlugin.emptyLock
    get() = IdleFileLock(temporaryFolder.newFile().toPath())

  private val IdePlugin.pluginInfo
    get() = createMockPluginInfo(pluginId!!, pluginVersion!!)

  private val IdePlugin.details
    get() = PluginDetails(pluginInfo, idePlugin = this, pluginWarnings = emptyList(), emptyClassesLocations, emptyLock)

  @Language("JSON")
  private val productInfoJsonIU243 = """
    {
      "name": "IntelliJ IDEA",
      "version": "2024.3",
      "buildNumber": "243.21565.193",
      "productCode": "IU",
      "envVarBaseName": "IDEA",
      "dataDirectoryName": "IntelliJIdea2024.3",
      "svgIconPath": "../bin/idea.svg",
      "productVendor": "JetBrains",
      "launch": [
        {
          "os": "macOS",
          "arch": "aarch64",
          "launcherPath": "../MacOS/idea",
          "javaExecutablePath": "../jbr/Contents/Home/bin/java",
          "vmOptionsFilePath": "../bin/idea.vmoptions",
          "bootClassPathJarNames": [
            "app.jar",
            "idea_rt.jar"
          ]      
        }
      ],        
      "bundledPlugins": [],
      "modules": [],
      "layout": []
    } 
  """.trimIndent()
}