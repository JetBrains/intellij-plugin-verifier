package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.problems.InvalidDescriptorProblem
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.ProblemSolutionHint
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginContentDescriptor

const val MIN_DESCRIPTION_LENGTH = 40

class DescriptionNotStartingWithLatinCharacters : InvalidDescriptorProblem(
  descriptorPath = "description",
  detailedMessage = "The plugin description must start with Latin characters and have at least $MIN_DESCRIPTION_LENGTH characters."
) {
  override val level
    get() = Level.UNACCEPTABLE_WARNING
}

class HttpLinkInDescription(link: String) : InvalidDescriptorProblem(
  descriptorPath = "description",
  detailedMessage = "All the links in the plugin description must be HTTPS: $link."
) {
  override val level
    get() = Level.UNACCEPTABLE_WARNING
}

class ServiceExtensionPointPreloadNotSupported(
  private val serviceType: IdePluginContentDescriptor.ServiceType
) : PluginProblem() {
  private val extensionPointPrefix = "com.intellij"

  override val level
    get() = Level.UNACCEPTABLE_WARNING

  override val hint = ProblemSolutionHint(
    documentationUrl = "https://plugins.jetbrains.com/docs/intellij/plugin-listeners.html"
  )
  override val message
    get() = "Service preloading is deprecated in the <${serviceType.toXmlElement()}> element. Remove the 'preload' " +
            "attribute and migrate to listeners, see https://plugins.jetbrains.com/docs/intellij/plugin-listeners.html."

  private fun IdePluginContentDescriptor.ServiceType.toXmlElement(): String = "$extensionPointPrefix." + when (this) {
    IdePluginContentDescriptor.ServiceType.PROJECT -> "projectService"
    IdePluginContentDescriptor.ServiceType.APPLICATION -> "applicationService"
    IdePluginContentDescriptor.ServiceType.MODULE -> "moduleService"
  }
}

class StatusBarWidgetFactoryExtensionPointIdMissing(private val implementationClassFqn: String) : PluginProblem() {
  private val extensionPointName = "com.intellij.statusBarWidgetFactory"

  override val level
    get() = Level.UNACCEPTABLE_WARNING

  override val message
    get() = "The extension point in the <${extensionPointName}> element must have 'id' attribute set with the same " +
            "value returned from the getId() method of the $implementationClassFqn implementation."
}

class NoDependencies(descriptorPath: String) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "Plugin has no dependencies. Please check the documentation: https://plugins.jetbrains.com/docs/intellij/plugin-compatibility.html"
) {
  override val level
    get() = Level.UNACCEPTABLE_WARNING
}