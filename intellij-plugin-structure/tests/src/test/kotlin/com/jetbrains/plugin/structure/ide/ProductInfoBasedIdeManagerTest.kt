package com.jetbrains.plugin.structure.ide

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import ch.qos.logback.core.spi.AppenderAttachable
import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.ide.IdeManagerImpl.PlatformResourceResolver
import com.jetbrains.plugin.structure.ide.plugin.PluginIdExtractor
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.plugin.BundledPluginManager
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.plugin.PluginArtifactPath
import com.jetbrains.plugin.structure.intellij.plugin.PluginIdProvider
import com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule
import com.jetbrains.plugin.structure.intellij.problems.AnyProblemToWarningPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.jar.PLUGIN_XML
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.nio.file.Path
import java.time.LocalDate

class ProductInfoBasedIdeManagerTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var ideRoot: Path

  @Before
  fun setUp() {
    ideRoot = MockIdeBuilder(temporaryFolder).buildIdeaDirectory()
  }

  @Test
  fun `create IDE manager from mock IDE`() {
    val ideManager = ProductInfoBasedIdeManager()
    val ide = ideManager.createIde(ideRoot)
    assertIdeAndPluginsIsCreated(ide)
  }

  @Test
  fun `create IDE manager from mock IDE on macOS`() {
    val ideManager = ProductInfoBasedIdeManager()
    val ideRoot = MockIdeBuilder(temporaryFolder, folderSuffix = "-mac").buildCoreIdeDirectoryForMacOs(miniProductInfoJson)
    val ide = ideManager.createIde(ideRoot)
    assertEquals(1, ide.bundledPlugins.size)
    val corePlugin = ide.findPluginById("com.intellij")
    assertNotNull(corePlugin)
  }

  @Test
  fun `create IDE manager from mock IDE on macOS with paths in layout component that are not available in the filesystem`() {
    val appender = ListAppender<ILoggingEvent>()
    @Suppress("UNCHECKED_CAST")
    val logger = LoggerFactory.getLogger(ProductInfoBasedIdeManager::class.java)
      as AppenderAttachable<ILoggingEvent>
    logger.addAppender(appender)
    appender.start()

    val ideManager = ProductInfoBasedIdeManager()
    val ideRoot = MockIdeBuilder(temporaryFolder, folderSuffix = "-mac").buildCoreIdeDirectoryForMacOs {
      """
      {
        "name": "IntelliJ IDEA",
        "version": "2024.2",
        "buildNumber": "242.10180.25",
        "productCode": "IU",
        "dataDirectoryName": "IntelliJIdea2024.2",
        "svgIconPath": "bin/idea.svg",
        "productVendor": "JetBrains",
        "launch": [],
        "bundledPlugins": [],
        "modules": [],
        "fileExtensions": [],
        "layout": [
          {
            "name": "org.jetbrains.plugins.emojipicker",
            "kind": "plugin",
            "classPath": [
              "plugins/emojipicker/lib/emojipicker.jar"
            ]
          }
        ]
      }
      """.trimIndent()
    }
    val ide = ideManager.createIde(ideRoot)
    assertEquals(1, ide.bundledPlugins.size)
    val corePlugin = ide.findPluginById("com.intellij")
    assertNotNull(corePlugin)

    assertTrue(appender.list.none {
      it.formattedMessage == "Following 1 plugins could not be created: " +
        "org.jetbrains.plugins.emojipicker' from 'plugins/emojipicker/lib/emojipicker.jar'"
    })

    appender.stop()
    logger.detachAppender(appender)
  }

  @Test
  fun `create nonIDEA IDE manager from mock IDE`() {
    val ideManager = ProductInfoBasedIdeManager()
    val ideRoot = MockRiderBuilder(temporaryFolder).buildIdeaDirectory()
    val ide = ideManager.createIde(ideRoot)

    val ideCore = ide.findPluginById("com.intellij")
    assertNotNull(ideCore)
    with(ideCore!!) {
      with(definedModules) {
        assertEquals(1, size)
        assertEquals("com.intellij.modules.rider", definedModules.first())
      }
    }
    val riderModule = ide.findPluginByModule("com.intellij.modules.rider")
    assertNotNull(riderModule)
    riderModule!!
    assertTrue(ideCore.pluginId == riderModule.pluginId)
  }

  @Test
  fun `create IDE manager with missing version suffix`() {
    val ideManager = ProductInfoBasedIdeManager()
    val ideRoot = MockIdeBuilder(temporaryFolder, "-missing-version-suffix").buildIdeaDirectory {
      omitVersionSuffix()
    }
    val ide = ideManager.createIde(ideRoot)
    assertIdeAndPluginsIsCreated(ide)
  }

  @Test
  fun `create IDE manager with nonexistent paths in layout component`() {
    val ideManager = ProductInfoBasedIdeManager()
    val ideRoot = MockIdeBuilder(temporaryFolder, "-missing-version-suffix").buildIdeaDirectory {
      //language=JSON
      layout = """
        {
          "name": "AngularJS",
          "kind": "plugin",
          "classPath": [
            "../../../../../angular-pha3hahghohlu6A.jar"
          ]
        }
      """.trimIndent()
    }
    val ide = ideManager.createIde(ideRoot)
    assertIdeAndPluginsIsCreated(ide)
  }

  @Test
  fun `create IDE manager with plugins in 'plugins' dir, but not declared in the product info jSON`() {
    val ideRoot = MockIdeBuilder(temporaryFolder, "-missing-layout-entry-for-plugin").buildIdeaDirectory {
      replaceLayout = true
      //language=JSON
      layout = ""
    }

    val pluginIdExtractor = PluginIdExtractor()
    // TODO unify this into a single interface
    val pluginIdProvider = object : PluginIdProvider {
      override fun getPluginId(pluginDescriptorStream: InputStream): String {
        return pluginIdExtractor.extractId(pluginDescriptorStream)
      }
    }

    fun getIdePluginManager(idePath: Path): ResourceResolver {
      return PlatformResourceResolver.of(idePath)
    }

    val additionalPluginReader = object : ProductInfoBasedIdeManager.PluginReader {
      private val bundledPluginManager = BundledPluginManager(pluginIdProvider)

      override fun readPlugins(idePath: Path, productInfo: ProductInfo, ideVersion: IdeVersion): List<IdePlugin> {
        val resourceResolver = getIdePluginManager(idePath)

        val identifiersInPluginsDir = bundledPluginManager.getBundledPluginIds(idePath)
        val identifiersInLayout = productInfo.layout.map { it.name }

        val filtered = filter(identifiersInPluginsDir, identifiersInLayout)
        return if (filtered.isNotEmpty()) {
          filtered.mapNotNull {
            try {
              createBundledPluginExceptionally(idePath, it.path, resourceResolver, PLUGIN_XML, ideVersion)
            } catch (e: InvalidIdeException) {
              // FIXME replace with logger
              println(e.reason)
              null
            }
          }
        } else {
          emptyList()
        }
      }

      private fun filter(artifacts: Set<PluginArtifactPath>, layoutIdentifiers: List<String>): List<PluginArtifactPath> {
        return artifacts.filterNot { it.pluginId in layoutIdentifiers }
      }

      // TODO copied from IdeManagerImpl
      @Throws(InvalidIdeException::class)
      private fun createBundledPluginExceptionally(
        idePath: Path,
        pluginFile: Path,
        resourceResolver: ResourceResolver,
        descriptorPath: String,
        ideVersion: IdeVersion
      ): IdePlugin = when (val creationResult = IdePluginManager
        .createManager(resourceResolver)
        .createBundledPlugin(pluginFile, ideVersion, descriptorPath, problemResolver = AnyProblemToWarningPluginCreationResultResolver)
      ) {
        is PluginCreationSuccess -> creationResult.plugin
        is PluginCreationFail -> throw InvalidIdeException(
          idePath,
          "Plugin '${idePath.relativize(pluginFile)}' is invalid: " +
            creationResult.errorsAndWarnings.filter { it.level == PluginProblem.Level.ERROR }.joinToString { it.message }
        )
      }
    }

    val ideManager = ProductInfoBasedIdeManager(additionalPluginReader = additionalPluginReader)

    val ide = ideManager.createIde(ideRoot)
    assertEquals(3, ide.bundledPlugins.size)
    val javaPlugin = ide.findPluginById("com.intellij.java")
    assertNotNull(javaPlugin)
    assertTrue(javaPlugin is IdePluginImpl)
    with(javaPlugin as IdePluginImpl) {
      assertEquals("Java", pluginName)
    }

    val cwmPlugin = ide.findPluginById("com.jetbrains.codeWithMe")
    assertNotNull(cwmPlugin)
    assertTrue(cwmPlugin is IdePluginImpl)
    with(cwmPlugin as IdePluginImpl) {
      assertEquals("Code With Me", pluginName)
    }
  }

  @Test
  fun `create IDE manager with paths in layout component that are not available in the filesystem`() {
    val ideManager = ProductInfoBasedIdeManager()
    val ideRoot = MockIdeBuilder(temporaryFolder, "-missing-layout-component").buildIdeaDirectory {
      //language=JSON
      layout = """
        {
          "name": "org.jetbrains.plugins.emojipicker",
          "kind": "plugin",
          "classPath": [
            "plugins/emojipicker/lib/emojipicker.jar"
          ]
        }
      """.trimIndent()
    }
    val ide = ideManager.createIde(ideRoot)
    assertEquals(5, ide.bundledPlugins.size)
    val emojiPickerPlugin = ide.findPluginById("org.jetbrains.plugins.emojipicker")
    assertNull(emojiPickerPlugin)
  }

  @Test
  fun `create IDE manager where plugin xincludes descriptor, but that descriptor is searched in an a plugin that is earlier declared in Product Info, but not present in the filesystem`() {
    val ideManager = ProductInfoBasedIdeManager()
    // 'org.jetbrains.plugins.emojipicker' is declared before 'com.intellij.java', however, not available in the filesystem.
    val ideRoot = MockIdeBuilder(temporaryFolder, "-missing-layout-component-java-plugin").buildIdeaDirectory {
      layout = """
        {
          "name": "org.jetbrains.plugins.emojipicker",
          "kind": "plugin",
          "classPath": [
            "plugins/emojipicker/lib/emojipicker.jar"
          ]
        },        
        {
          "name": "com.intellij.java",
          "kind": "plugin",
          "classPath": [
            "plugins/java/lib/java-impl.jar"
          ]
        }        
      """.trimIndent()
    }
    val ide = ideManager.createIde(ideRoot)
    assertEquals(6, ide.bundledPlugins.size)

    val emojiPickerPlugin = ide.findPluginById("org.jetbrains.plugins.emojipicker")
    assertNull(emojiPickerPlugin)

    val javaPlugin = ide.findPluginById("com.intellij.java")
    assertNotNull(javaPlugin)
    with(javaPlugin!!) {
      assertEquals("242.10180.25", pluginVersion)
    }
  }

  private fun assertIdeAndPluginsIsCreated(ide: Ide) {
    assertEquals(5, ide.bundledPlugins.size)
    val uiPlugin = ide.findPluginById("intellij.notebooks.ui")
    assertNotNull(uiPlugin)
    assertTrue(uiPlugin is IdeModule)
    with(uiPlugin as IdeModule) {
      assertEquals(1, classpath.size)
      assertEquals(1, moduleDependencies.size)
      assertEquals(1, resources.size)
    }

    val visualizationPlugin = ide.findPluginById("intellij.notebooks.visualization")
    assertNotNull(visualizationPlugin)
    assertTrue(visualizationPlugin is IdeModule)

    with(visualizationPlugin as IdeModule) {
      assertEquals(1, classpath.size)
      assertEquals(1, moduleDependencies.size)
      assertEquals(1, resources.size)
    }

    val javaFeaturesTrainer = ide.findPluginById("intellij.java.featuresTrainer")
    assertNotNull(javaFeaturesTrainer)
    assertTrue(javaFeaturesTrainer is IdeModule)
    with(javaFeaturesTrainer as IdeModule) {
      assertEquals(1, classpath.size)
      assertEquals(0, moduleDependencies.size)
      assertEquals(1, resources.size)
    }

    val ideCore = ide.findPluginById("com.intellij")
    assertNotNull(ideCore)
    with(ideCore!!) {
      assertEquals(4, definedModules.size)
    }

    val codeWithMe = ide.findPluginById("com.jetbrains.codeWithMe")
    assertNotNull(codeWithMe)
    with(codeWithMe!!) {
      assertNotNull(productDescriptor)
      val productDescriptor = productDescriptor!!
      assertEquals(LocalDate.of(4000, 1, 1), productDescriptor.releaseDate)
    }
  }

  private val miniProductInfoJson = """
      {
        "name": "IntelliJ IDEA",
        "version": "2024.2",
        "buildNumber": "242.10180.25",
        "productCode": "IU",
        "dataDirectoryName": "IntelliJIdea2024.2",
        "svgIconPath": "bin/idea.svg",
        "productVendor": "JetBrains",
        "launch": [],
        "bundledPlugins": [],
        "modules": [],
        "fileExtensions": [],
        "layout": []
      }
      """.trimIndent()
}