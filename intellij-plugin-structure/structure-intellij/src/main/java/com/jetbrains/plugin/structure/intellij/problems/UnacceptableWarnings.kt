package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.InvalidDescriptorProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginContentDescriptor

const val MIN_DESCRIPTION_LENGTH = 40

class ShortOrNonLatinDescription : InvalidDescriptorProblem("description") {

  override val level
    get() = Level.UNACCEPTABLE_WARNING

  override val detailedMessage
    get() = "Please provide a long-enough English description."
}

class HttpLinkInDescription(private val link: String) : InvalidDescriptorProblem("description") {

  override val level
    get() = Level.UNACCEPTABLE_WARNING

  override val detailedMessage
    get() = "All links in description must be HTTPS: $link"
}

class ServiceExtensionPointPreloadNotSupported(private val serviceType: IdePluginContentDescriptor.ServiceType) : PluginProblem() {
  private val extensionPointPrefix = "com.intellij"

  override val level
    get() = Level.UNACCEPTABLE_WARNING

  override val message
    get() = "Service preloading is deprecated in the <${serviceType.toXmlElement()}> element. Consider removing the 'preload' attribute and migrating to listeners, see https://plugins.jetbrains.com/docs/intellij/plugin-listeners.html"

  private fun IdePluginContentDescriptor.ServiceType.toXmlElement(): String = "$extensionPointPrefix." + when (this) {
    IdePluginContentDescriptor.ServiceType.PROJECT -> "projectService"
    IdePluginContentDescriptor.ServiceType.APPLICATION -> "applicationService"
    IdePluginContentDescriptor.ServiceType.MODULE -> "moduleService"
  }
}

class StatusBarWidgetFactoryExtensionPointIdMissing(private val implementationClassFqn: String) : PluginProblem() {
  private val extensionPointPrefix = "com.intellij"

  override val level
    get() = Level.UNACCEPTABLE_WARNING

  override val message
    get() = "Extension Point in the <${extensionPointPrefix}.statusBarWidgetFactory> element " +
      "must have 'id' attribute set for the [$implementationClassFqn] implementation. " +
      "Furthermore, it must match the value returned from the getId() method of the implementation."
}