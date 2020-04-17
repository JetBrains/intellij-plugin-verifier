/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

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