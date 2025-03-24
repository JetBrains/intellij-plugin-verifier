package com.jetbrains.plugin.structure.ide.classes.resolver

import com.jetbrains.plugin.structure.base.utils.createParentDirs
import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.plugin.structure.classes.resolvers.Resolver.ReadMode
import com.jetbrains.plugin.structure.ide.classes.IdeResolverConfiguration
import com.jetbrains.plugin.structure.ide.layout.MissingLayoutFileMode
import com.jetbrains.plugin.structure.intellij.platform.Launch
import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent
import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent.Plugin
import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent.PluginAlias
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.plugin.Classpath
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.ModuleV2Dependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.MockIde
import com.jetbrains.plugin.structure.mocks.MockIdePlugin
import net.bytebuddy.ByteBuddy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PluginDependencyFilteredResolverTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var ideRoot: Path

  private lateinit var ideaCorePluginFile: Path

  private lateinit var ideaCorePlugin: IdePlugin
  private lateinit var javaPlugin: IdePlugin
  private lateinit var jsonPlugin: IdePlugin

  private lateinit var byteBuddy: ByteBuddy

  private fun zip(zipPath: Path, fullyQualifiedName: String) {
    ZipOutputStream(FileOutputStream(zipPath.toFile())).use {
      val zipEntryName = fullyQualifiedName.replace('.', '/') + ".class"
      it.putNextEntry(ZipEntry(zipEntryName))
      it.write(emptyClass(fullyQualifiedName))
      it.closeEntry()
    }
  }

  private fun emptyClass(fullyQualifiedName: String): ByteArray {
    return byteBuddy
      .subclass(Object::class.java)
      .name(fullyQualifiedName)
      .make()
      .bytes
  }

  private fun IdePlugin.writeEmptyClass(fullyQualifiedName: String): IdePlugin {
    originalFile?.let { pluginArtifact: Path ->
      zip(pluginArtifact, fullyQualifiedName)
    }
    return this
  }

  private val resolverConfiguration = IdeResolverConfiguration(ReadMode.FULL, MissingLayoutFileMode.SKIP_AND_WARN)

  @Before
  fun setUp() {
    byteBuddy = ByteBuddy()

    ideRoot = temporaryFolder.newFolder("idea").toPath()

    ideaCorePluginFile = temporaryFolder.newTemporaryFile("idea/lib/product.jar")
    ideaCorePlugin = MockIdePlugin(
      pluginId = "com.intellij",
      pluginName = "IDEA CORE",
      originalFile = ideaCorePluginFile,
      definedModules = setOf(
        "com.intellij.modules.platform",
        "com.intellij.modules.lang",
        "com.intellij.modules.java",
      ),
      classpath = Classpath.of(listOf(ideaCorePluginFile))
    ).also {
      it.writeEmptyClass("com.intellij.openapi.editor.Caret")
    }

    javaPlugin = MockIdePlugin(
      pluginId = "com.intellij.java",
      pluginName = "Java",
      originalFile = temporaryFolder.newTemporaryFile("idea/plugins/java/lib/java-impl.jar"),
      definedModules = setOf(
        "com.intellij.modules.java",
      ),
      dependencies = listOf(
        ModuleV2Dependency("com.intellij.modules.lang")
      )
    )

    jsonPlugin = MockIdePlugin(
      pluginId = "com.intellij.modules.json",
      pluginName = "JSON",
      originalFile = temporaryFolder.newTemporaryFile("idea/plugins/json/lib/json.jar")
    )
  }

  @Test
  fun `plugin dependency-based resolvers are resolved`() {
    val ideVersion = IdeVersion.createIdeVersion("IU-243.12818.47")

    val plugin = MockIdePlugin(
      pluginId = "com.example.somePlugin",
      dependencies = listOf(
        PluginDependencyImpl(/* id = */ "com.intellij.modules.platform",
          /* isOptional = */ false,
          /* isModule = */ true
        ),
        PluginDependencyImpl(/* id = */ "com.intellij.modules.json",
          /* isOptional = */ false,
          /* isModule = */ true
        ),
      )
    )

    val productInfo = ProductInfo(
      name = "IntelliJ IDEA",
      version = "2024.3",
      versionSuffix = "EAP",
      buildNumber = ideVersion.asStringWithoutProductCode(),
      productCode = "IU",
      dataDirectoryName = "IntelliJIdea2024.3",
      productVendor = "JetBrains",
      launch = emptyList(),
      svgIconPath = "bin/idea.svg",
      modules = emptyList(),
      bundledPlugins = emptyList(),
      layout = listOf(
        PluginAlias("com.intellij.modules.platform"),
        Plugin("com.intellij.modules.json", listOf("plugins/json/lib/json.jar")),
        Plugin("Git4Idea", listOf("plugins/vcs-git/lib/vcs-git.jar", "plugins/vcs-git/lib/git4idea-rt.jar")),
      ),
    )

    productInfo.createEmptyLayoutComponentPaths(ideRoot)

    val ide = MockIde(ideVersion, ideRoot, bundledPlugins = listOf(ideaCorePlugin, jsonPlugin))

    val productInfoClassResolver = ProductInfoClassResolver(productInfo, ide, resolverConfiguration)
    val pluginDependencyFilteredResolver = PluginDependencyFilteredResolver(plugin, productInfoClassResolver)

    with(pluginDependencyFilteredResolver) {
      // pluginAlias has no classpath, hence no resolver
      assertFalse(containsResolverName("com.intellij.modules.platform"))
      // JSON plugin is declared
      assertTrue(containsResolverName("com.intellij.modules.json"))
      // Git4Idea is not a plugin dependency
      assertFalse(containsResolverName("Git4Idea"))
    }
  }

  @Test
  fun `transitive plugin dependencies are filtered`() {
    val ideVersion = IdeVersion.createIdeVersion("IU-243.12818.47")

    val plugin = MockIdePlugin(
      pluginId = "com.example.somePlugin",
      vendor = "JetBrains",
      dependencies = listOf(
        PluginDependencyImpl(/* id = */ "com.intellij.modules.lang",
          /* isOptional = */ false,
          /* isModule = */ true
        ),
        PluginDependencyImpl(/* id = */ "com.intellij.modules.json",
          /* isOptional = */ false,
          /* isModule = */ true
        ),
      )
    )

    val productInfo = ProductInfo(
      name = "IntelliJ IDEA",
      version = "2024.3",
      versionSuffix = "EAP",
      buildNumber = ideVersion.asStringWithoutProductCode(),
      productCode = "IU",
      dataDirectoryName = "IntelliJIdea2024.3",
      productVendor = "JetBrains",
      svgIconPath = "bin/idea.svg",
      modules = emptyList(),
      bundledPlugins = emptyList(),
      layout = listOf(
        Plugin("com.intellij.modules.json", listOf("plugins/json/lib/json.jar")),
        PluginAlias("com.intellij.modules.lang"),
      ),
      launch = listOf(
        Launch(bootClassPathJarNames = listOf("product.jar"))
      )
    )

    productInfo.createEmptyLayoutComponentPaths(ideRoot)

    val bundledPlugins = listOf(ideaCorePlugin, jsonPlugin)
    val ide = MockIde(ideVersion, ideRoot, bundledPlugins)

    val productInfoClassResolver = ProductInfoClassResolver(productInfo, ide, resolverConfiguration)
    val pluginDependencyFilteredResolver = PluginDependencyFilteredResolver(plugin, productInfoClassResolver)

    val editorCaretClassName = "com/intellij/openapi/editor/Caret"
    val editorCaretClassResolution = pluginDependencyFilteredResolver.resolveClass(editorCaretClassName)
    assertTrue(
      "Class '$editorCaretClassName' must be 'Found', but is '${editorCaretClassResolution.javaClass}'",
      editorCaretClassResolution is ResolutionResult.Found
    )
  }

  @Test
  fun `two-tier transitive plugin dependencies are filtered`() {
    val ideVersion = IdeVersion.createIdeVersion("IU-243.12818.47")

    val plugin = MockIdePlugin(
      pluginId = "com.example.somePlugin",
      vendor = "JetBrains",
      dependencies = listOf(
        PluginDependencyImpl(/* id = */ "com.intellij.java",
          /* isOptional = */ false,
          /* isModule = */ false
        ),
        PluginDependencyImpl(/* id = */ "com.intellij.modules.json",
          /* isOptional = */ false,
          /* isModule = */ true
        ),
      )
    )

    val productInfo = ProductInfo(
      name = "IntelliJ IDEA",
      version = "2024.3",
      versionSuffix = "EAP",
      buildNumber = ideVersion.asStringWithoutProductCode(),
      productCode = "IU",
      dataDirectoryName = "IntelliJIdea2024.3",
      productVendor = "JetBrains",
      svgIconPath = "bin/idea.svg",
      modules = emptyList(),
      bundledPlugins = emptyList(),
      layout = listOf(
        Plugin("com.intellij.modules.json", listOf("plugins/json/lib/json.jar")),
        Plugin("com.intellij.java", listOf("plugins/java/lib/java-impl.jar")),
        PluginAlias("com.intellij.modules.lang"),
      ),
      launch = listOf(
        Launch(bootClassPathJarNames = listOf("product.jar"))
      )
    )

    productInfo.createEmptyLayoutComponentPaths(ideRoot)

    val bundledPlugins = listOf(ideaCorePlugin, jsonPlugin, javaPlugin)
    val ide = MockIde(ideVersion, ideRoot, bundledPlugins)

    val productInfoClassResolver = ProductInfoClassResolver(productInfo, ide, resolverConfiguration)
    val pluginDependencyFilteredResolver = PluginDependencyFilteredResolver(plugin, productInfoClassResolver)

    val editorCaretClassName = "com/intellij/openapi/editor/Caret"
    val editorCaretClassResolution = pluginDependencyFilteredResolver.resolveClass(editorCaretClassName)
    assertTrue(
      "Class '$editorCaretClassName' must be 'Found', but is '${editorCaretClassResolution.javaClass}'",
      editorCaretClassResolution is ResolutionResult.Found
    )
  }

  @Test
  fun `multiple plugin resolvers`() {
    val ideVersion = IdeVersion.createIdeVersion("IU-243.12818.47")

    val plugin = MockIdePlugin(
      pluginId = "com.example.somePlugin",
      vendor = "JetBrains",
      dependencies = listOf(
        PluginDependencyImpl(/* id = */ "com.intellij.modules.lang",
          /* isOptional = */ false,
          /* isModule = */ true
        ),
        PluginDependencyImpl(/* id = */ "com.intellij.modules.json",
          /* isOptional = */ false,
          /* isModule = */ true
        ),
      )
    )

    val productInfo = ProductInfo(
      name = "IntelliJ IDEA",
      version = "2024.3",
      versionSuffix = "EAP",
      buildNumber = ideVersion.asStringWithoutProductCode(),
      productCode = "IU",
      dataDirectoryName = "IntelliJIdea2024.3",
      productVendor = "JetBrains",
      svgIconPath = "bin/idea.svg",
      modules = emptyList(),
      bundledPlugins = emptyList(),
      layout = listOf(
        Plugin("com.intellij.modules.json", listOf("plugins/json/lib/json.jar")),
        PluginAlias("com.intellij.modules.lang"),
      ),
      launch = listOf(
        Launch(bootClassPathJarNames = listOf("product.jar"))
      )
    )

    productInfo.createEmptyLayoutComponentPaths(ideRoot)

    val ide = MockIde(ideVersion, ideRoot, bundledPlugins = listOf(ideaCorePlugin, jsonPlugin))

    val productInfoClassResolver = ProductInfoClassResolver(productInfo, ide, resolverConfiguration)

    val resolverProvider = CachingPluginDependencyResolverProvider(ide)
    val pluginResolver = PluginDependencyFilteredResolver(plugin, productInfoClassResolver, resolverProvider)
    val anotherPluginResolver = PluginDependencyFilteredResolver(plugin, productInfoClassResolver, resolverProvider)

    assertTrue("Same resolvers should contain same child resolvers", pluginResolver.hasSameDelegate(
      anotherPluginResolver
    ))
  }

  private fun ProductInfo.createEmptyLayoutComponentPaths(ideRoot: Path) {
    layout
      .flatMap { if (it is LayoutComponent.Classpathable) it.getClasspath() else emptyList() }
      .map { ideRoot.resolve(it) }
      .map {
        it.apply { createParentDirs() }
      }
      .forEach {
        it.createEmptyZip()
      }
  }

  private fun Path.createEmptyZip() {
    ZipOutputStream(Files.newOutputStream(this)).use {}
  }

  private fun TemporaryFolder.newTemporaryFile(filePath: String): Path {
    val pathComponents = filePath.split("/")
    val dirComponents = pathComponents.dropLast(1).toTypedArray()
    if (dirComponents.isEmpty()) {
      throw IllegalArgumentException("Cannot create temporary file '$filePath'")
    }
    val fileComponent = pathComponents.last()
    val folder: File = newFolder(*dirComponents)
    return File(folder, fileComponent).toPath()
  }
}