/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

interface IdePluginContentDescriptor {
  enum class Os {
    mac, linux, windows, unix, freebsd
  }

  enum class ListenerType {
    PROJECT, APPLICATION
  }

  enum class ServiceType {
    PROJECT, APPLICATION, MODULE
  }

  enum class PreloadMode {
    TRUE, FALSE, AWAIT, NOT_HEADLESS, NOT_LIGHT_EDIT
  }

  enum class ClientKind {
    ALL, GUEST, LOCAL, CONTROLLER, OWNER, REMOTE, FRONTEND
  }

  val services: List<ServiceDescriptor>
  val components: List<ComponentConfig>
  val listeners: List<ListenerDescriptor>
  val extensionPoints: List<ExtensionPoint>

  data class ExtensionPoint(val extensionPointName: String, val isDynamic: Boolean)
  data class ComponentConfig(val interfaceClass: String?, val implementationClass: String)
  data class ListenerDescriptor(
    val topicName: String,
    val className: String,
    val type: ListenerType,
    val activeInTestMode: Boolean,
    val activeInHeadlessMode: Boolean,
    val os: Os?
  )

  data class ServiceDescriptor(
    val serviceInterface: String?,
    val serviceImplementation: String?,
    val type: ServiceType,
    val testServiceImplementation: String?,
    val headlessImplementation: String?,
    val overrides: Boolean?,
    val configurationSchemaKey: String?,
    val preload: PreloadMode = PreloadMode.FALSE,
    val client: ClientKind?,
    val os: Os?
  )
}

data class MutableIdePluginContentDescriptor(
  override val services: MutableList<IdePluginContentDescriptor.ServiceDescriptor> = arrayListOf(),
  override val components: MutableList<IdePluginContentDescriptor.ComponentConfig> = arrayListOf(),
  override val listeners: MutableList<IdePluginContentDescriptor.ListenerDescriptor> = arrayListOf(),
  override val extensionPoints: MutableList<IdePluginContentDescriptor.ExtensionPoint> = arrayListOf()
) : IdePluginContentDescriptor