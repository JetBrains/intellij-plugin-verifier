package com.jetbrains.plugin.structure.intellij.plugin

data class ExtensionPoint(
  val extensionPointName: String,
  val isDynamic: Boolean
)

data class ComponentConfig(
  val interfaceClass: String?,
  val implementationClass: String
)

data class ListenerDescriptor(
  val topicName: String,
  val className: String
)

data class ServiceDescriptor(
  val serviceInterface: String?,
  val serviceImplementation: String?
)

data class ContainerDescriptor(
  val services: MutableList<ServiceDescriptor> = arrayListOf(),
  val components: MutableList<ComponentConfig> = arrayListOf(),
  val listeners: MutableList<ListenerDescriptor> = arrayListOf(),
  val extensionPoints: MutableList<ExtensionPoint> = arrayListOf()
)