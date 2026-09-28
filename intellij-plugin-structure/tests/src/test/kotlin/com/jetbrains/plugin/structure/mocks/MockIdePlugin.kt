package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.base.plugin.ThirdPartyDependency
import com.jetbrains.plugin.structure.intellij.plugin.*
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.validation.MockIdePluginValidator.Companion.assertValid
import org.jdom2.Document
import org.jdom2.Element
import java.nio.file.Path

data class MockIdePlugin(
  override val pluginId: String? = null,
  override val pluginName: String? = pluginId,
  override val pluginVersion: String? = null,
  override val description: String? = null,
  override val url: String? = null,
  override val vendor: String? = null,
  override val vendorEmail: String? = null,
  override val vendorUrl: String? = null,
  override val changeNotes: String? = null,
  override val icons: List<PluginIcon> = emptyList(),
  override val productDescriptor: ProductDescriptor? = null,
  override val dependsList: List<DependsPluginDependency> = emptyList(),
  override val pluginMainModuleDependencies: List<PluginMainModuleDependency> = emptyList(),
  override val contentModuleDependencies: List<ContentModuleDependency> = emptyList(),
  override val incompatibleWith: List<String> = emptyList(),
  override val underlyingDocument: Document = Document(Element("idea-plugin")),
  override val optionalDescriptors: List<OptionalPluginDescriptor> = emptyList(),
  override val extensions: Map<String, List<Element>> = hashMapOf(),
  override val sinceBuild: IdeVersion = IdeVersion.createIdeVersion("IU-163.1"),
  override val untilBuild: IdeVersion? = null,
  override val pluginAliases: Set<String> = emptySet(),
  override val originalFile: Path? = null,
  override val appContainerDescriptor: IdePluginContentDescriptor = MutableIdePluginContentDescriptor(),
  override val projectContainerDescriptor: IdePluginContentDescriptor = MutableIdePluginContentDescriptor(),
  override val moduleContainerDescriptor: IdePluginContentDescriptor = MutableIdePluginContentDescriptor(),
  override val thirdPartyDependencies: List<ThirdPartyDependency> = emptyList(),
  @Deprecated("See IdePlugin::isV2")
  override val isV2: Boolean = false,
  override val hasPackagePrefix: Boolean = false,
  override val kotlinPluginMode: KotlinPluginMode = KotlinPluginMode.Implicit,
  override val classpath: Classpath = Classpath.EMPTY,
  override val contentModules: List<Module> = emptyList(),
  override val modulesDescriptors: List<ModuleDescriptor> = emptyList(),
) : IdePlugin {

  @Deprecated("contains mixed dependencies, including ones that belong to content modules; see dependsList, pluginMainModuleDependencies, contentModuleDependencies")
  override val dependencies: List<PluginDependency>
    get() = dependsList.map { it.asPluginDependency() } +
      contentModuleDependencies.map { ModuleV2Dependency(it.moduleName) } +
      pluginMainModuleDependencies.map { PluginV2Dependency(it.pluginId) }

  override val useIdeClassLoader = false
  override val isImplementationDetail = false
  override val moduleVisibility: ModuleVisibility = ModuleVisibility.PRIVATE
  override val hasDotNetPart: Boolean = false
  @Deprecated("use either pluginAliases or contentModules")
  override val definedModules: Set<String> = pluginAliases

  override val declaredThemes = emptyList<IdeTheme>()

  override fun isCompatibleWithIde(ideVersion: IdeVersion) =
    sinceBuild <= ideVersion && (untilBuild == null || ideVersion <= untilBuild)
}

fun idePlugin(id: String, configure: MockIdePluginBuilder.() -> Unit = {}): MockIdePlugin {
  return MockIdePluginBuilder(id).apply(configure).build()
}

class MockIdePluginBuilder(private val id: String) {
  private val dependsList = mutableListOf<DependsPluginDependency>()
  private val pluginMainModuleDependencies = mutableListOf<PluginMainModuleDependency>()
  private val contentModuleDependencies = mutableListOf<ContentModuleDependency>()

  fun depends(pluginId: String) {
    dependsList += MandatoryV1Dependency(pluginId)
  }

  fun depends(plugin: MockIdePlugin) {
    depends(plugin.requireId())
  }

  fun depends(dependency: DependsPluginDependency) {
    dependsList += dependency
  }

  fun optionalDepends(pluginId: String) {
    dependsList += DependsPluginDependency(pluginId, true)
  }

  fun optionalDepends(plugin: MockIdePlugin) {
    optionalDepends(plugin.requireId())
  }

  fun pluginDependency(pluginId: String) {
    pluginMainModuleDependencies += PluginMainModuleDependency(pluginId)
  }

  fun moduleDependency(moduleName: String, namespace: String = "jetbrains") {
    contentModuleDependencies += ContentModuleDependency(moduleName, namespace)
  }

  private fun MockIdePlugin.requireId() = requireNotNull(pluginId) { "Plugin ID is required to declare a dependency" }

  fun build() = MockIdePlugin(
    pluginId = id,
    dependsList = dependsList.toList(),
    pluginMainModuleDependencies = pluginMainModuleDependencies.toList(),
    contentModuleDependencies = contentModuleDependencies.toList(),
  ).assertValid()
}