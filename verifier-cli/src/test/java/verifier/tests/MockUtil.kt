package verifier.tests

import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.IdeVersion
import com.intellij.structure.domain.Plugin
import com.intellij.structure.domain.PluginDependency
import com.jetbrains.pluginverifier.api.JdkDescriptor
import org.jetbrains.intellij.plugins.internal.guava.collect.ArrayListMultimap
import org.jetbrains.intellij.plugins.internal.guava.collect.Multimap
import org.jetbrains.intellij.plugins.internal.jdom.Document
import org.jetbrains.intellij.plugins.internal.jdom.Element
import java.io.File
import java.nio.file.Files

object MockUtil {

  fun getJdkDescriptor(): JdkDescriptor.ByFile {
    val jdkPath = System.getenv("JAVA_HOME") ?: "/usr/lib/jvm/java-8-oracle"
    return JdkDescriptor.ByFile(jdkPath)
  }

  fun createMockPlugin(pluginId: String,
                       pluginVersion: String,
                       moduleDependencies: List<PluginDependency> = emptyList(),
                       dependencies: List<PluginDependency> = emptyList(),
                       definedModules: Set<String> = emptySet()): MockPlugin {
    val tempPluginDir = Files.createTempDirectory("mock-plugin").toFile()
    tempPluginDir.deleteOnExit()
    return MockPlugin(pluginId, pluginVersion, dependencies, moduleDependencies, definedModules, tempPluginDir)
  }

  fun createTestIde(ideVersion: IdeVersion,
                    bundledPlugins: List<Plugin> = emptyList(),
                    customPlugins: List<Plugin> = emptyList()): Ide {
    val tempIdeDir = Files.createTempDirectory("mock-ide").toFile()
    tempIdeDir.deleteOnExit()
    return MockIde(ideVersion, bundledPlugins, customPlugins, tempIdeDir)
  }

  class MockIde(private val ideVersion: IdeVersion,
                private val bundled: List<Plugin> = emptyList(),
                private val custom: List<Plugin> = emptyList(),
                private val ideFile: File) : Ide() {

    override fun getCustomPlugins(): List<Plugin> = custom

    override fun getBundledPlugins(): List<Plugin> = bundled

    override fun getVersion(): IdeVersion = ideVersion

    override fun getExpandedIde(plugin: Plugin): Ide = MockIde(ideVersion, bundled, custom + listOf(plugin), ideFile)

    override fun getIdePath(): File = ideFile

  }

  class MockPlugin(private val pluginId: String,
                   private val pluginVersion: String,
                   private val dependencies: List<PluginDependency>,
                   private val moduleDependencies: List<PluginDependency>,
                   private val definedModules: Set<String>,
                   private val pluginFile: File) : Plugin {
    override fun getPluginId(): String = pluginId

    override fun getPluginVersion(): String = pluginVersion

    override fun getModuleDependencies(): List<PluginDependency> = moduleDependencies

    override fun getVendor(): String = "vendor"

    override fun getChangeNotes(): String = "changeNotes"

    override fun getVendorLogo(): ByteArray = byteArrayOf()

    override fun getUnderlyingDocument(): Document = Document()

    override fun getAllClassesReferencedFromXml(): Set<String>? = emptySet()

    override fun getExtensions(): Multimap<String, Element> = ArrayListMultimap.create()

    override fun isCompatibleWithIde(ideVersion: IdeVersion?): Boolean = true

    override fun getDescription(): String = "description"

    override fun getUrl(): String = "url"

    override fun getVendorEmail(): String = "vendor email"

    override fun getVendorUrl(): String = "vendor url"

    override fun getVendorLogoUrl(): String = "vendorLogoUrl"

    override fun getPluginName(): String = "name"

    override fun getSinceBuild(): IdeVersion = IdeVersion.createIdeVersion("IU-171")

    override fun getUntilBuild(): IdeVersion = IdeVersion.createIdeVersion("IU-1")

    override fun getPluginFile(): File = pluginFile

    override fun getDefinedModules(): Set<String> = definedModules

    override fun getDependencies(): List<PluginDependency> = dependencies

    override fun getOptionalDescriptors(): Map<String, Plugin> = emptyMap()
  }
}
