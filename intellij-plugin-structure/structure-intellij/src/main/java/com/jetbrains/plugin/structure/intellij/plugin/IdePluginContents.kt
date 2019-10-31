package com.jetbrains.plugin.structure.intellij.plugin

import org.jdom2.Element

data class ExtensionPoint(
  val extensionPointName: String,
  val isDynamic: Boolean,
  val extensionElement: Element
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