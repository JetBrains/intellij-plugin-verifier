package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.base.plugin.ThirdPartyDependency
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.intellij.beans.PluginBean
import com.jetbrains.plugin.structure.intellij.plugin.KotlinPluginMode.Implicit
import com.jetbrains.plugin.structure.intellij.verifiers.ProblemRegistrar
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jdom2.Document
import org.jdom2.Element
import java.nio.file.Path

/**
 * An invalid plugin that was either not fully parsed or that has been parsed with syntax error in the descriptor.
 * Such placeholder class can be used in the verification engines to filter out specific verification rules
 * despite the incomplete or erroneous metadata from plugin descriptor.
 */
class InvalidPlugin(override val underlyingDocument: Document) : IdePlugin, StructurallyValidated, ProblemRegistrar {
  override var pluginId: String? = null
  override var pluginName: String? = null
  override var pluginVersion: String? = null
  override var url: String? = null
  override var changeNotes: String? = null
  override var description: String? = null
  override var vendor: String? = null
  override var vendorEmail: String? = null
  override var vendorUrl: String? = null
  override var icons: List<PluginIcon> = emptyList()
  override var thirdPartyDependencies: List<ThirdPartyDependency> = emptyList()

  override val sinceBuild: IdeVersion? = null
  override val untilBuild: IdeVersion? = null
  override val extensions: Map<String, List<Element>> = emptyMap()
  override val appContainerDescriptor: IdePluginContentDescriptor = MutableIdePluginContentDescriptor()
  override val projectContainerDescriptor: IdePluginContentDescriptor = MutableIdePluginContentDescriptor()
  override val moduleContainerDescriptor: IdePluginContentDescriptor = MutableIdePluginContentDescriptor()
  override val dependencies: List<PluginDependency> = emptyList()
  override val incompatibleWith: List<String> = emptyList()
  override val pluginAliases: Set<String> = emptySet()
  @Deprecated("use either pluginAliases or contentModules")
  override val definedModules: Set<String> = emptySet()
  override val optionalDescriptors: List<OptionalPluginDescriptor> = emptyList()
  override val modulesDescriptors: List<ModuleDescriptor> = emptyList()
  override val contentModules: List<Module> = emptyList()
  override val originalFile: Path? = null
  override val productDescriptor: ProductDescriptor? = null
  override val declaredThemes: List<IdeTheme> = emptyList()
  override val useIdeClassLoader: Boolean = false
  override val classpath: Classpath = Classpath.EMPTY
  override val isImplementationDetail: Boolean = false
  @Deprecated("See IdePlugin::isV2")
  override val isV2: Boolean = false
  override val hasPackagePrefix: Boolean = false
  override val kotlinPluginMode: KotlinPluginMode = Implicit
  override fun isCompatibleWithIde(ideVersion: IdeVersion): Boolean = false
  override val hasDotNetPart: Boolean = false
  override val problems: MutableList<PluginProblem> = mutableListOf()

  override fun registerProblem(problem: PluginProblem) {
    problems += problem
  }
}

/**
 * Create an instance of an invalid plugin with the most basic information.
 *
 * This allows creating a bare-bones plugin with invalid data.
 * Plugin problems associated with this invalid data might be reclassified by the [problem resolver](#problemResolver).
 */
internal fun newInvalidPlugin(bean: PluginBean, document: Document): InvalidPlugin {
  return InvalidPlugin(document).apply {
    pluginId = bean.id?.trim()
    pluginName = bean.name?.trim()
    bean.vendor?.let {
      vendor = if (it.name != null) it.name.trim() else null
      vendorUrl = it.url
      vendorEmail = it.email
    }
  }
}
