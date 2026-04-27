/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.cache

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

internal const val CACHE_FORMAT_VERSION = 1

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class CachedIdeDescriptor(
  val cacheVersion: Int = CACHE_FORMAT_VERSION,
  val ideVersion: String = "",
  val productInfoMtime: Long = 0L,
  val plugins: List<CachedPluginData> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class CachedPluginData(
  // Identity
  val pluginId: String? = null,
  val pluginName: String? = null,
  val pluginVersion: String? = null,
  val sinceBuild: String? = null,
  val untilBuild: String? = null,
  val originalFile: String? = null,
  // Classpath: paths stored relative to IDE root
  val classpath: List<CachedClasspathEntry> = emptyList(),
  // Dependencies
  val dependsList: List<CachedDependsData> = emptyList(),
  val contentModuleDependencies: List<CachedContentModuleDepData> = emptyList(),
  val pluginMainModuleDependencies: List<String> = emptyList(),
  val incompatibleWith: List<String> = emptyList(),
  // Module declarations
  val pluginAliases: Set<String> = emptySet(),
  val contentModules: List<CachedModuleData> = emptyList(),
  // Flags
  val isImplementationDetail: Boolean = false,
  val useIdeClassLoader: Boolean = false,
  val hasPackagePrefix: Boolean = false,
  val moduleVisibility: String = "PRIVATE",
  val kotlinK1Compatible: Boolean = true,
  val kotlinK2Compatible: Boolean = false,
  val hasDotNetPart: Boolean = false,
  // XML (option B): extensions and actions stored as XML strings; full document stored for underlyingDocument
  val underlyingDocumentXml: String = "",
  val extensionsXml: Map<String, List<String>> = emptyMap(),
  val actionsXml: List<String> = emptyList(),
  // Container descriptors
  val appContainerDescriptor: CachedContentDescriptor = CachedContentDescriptor(),
  val projectContainerDescriptor: CachedContentDescriptor = CachedContentDescriptor(),
  val moduleContainerDescriptor: CachedContentDescriptor = CachedContentDescriptor(),
  // Nested plugins
  val modulesDescriptors: List<CachedModuleDescriptorData> = emptyList(),
  val optionalDescriptors: List<CachedOptionalDescriptorData> = emptyList(),
  // Metadata
  val declaredThemes: List<CachedThemeData> = emptyList(),
  val productDescriptor: CachedProductDescriptorData? = null,
  val thirdPartyDependencies: List<CachedThirdPartyDependency> = emptyList(),
  val vendor: String? = null,
  val vendorEmail: String? = null,
  val vendorUrl: String? = null,
  val description: String? = null,
  val changeNotes: String? = null,
  val url: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class CachedClasspathEntry(
  val path: String = "",
  val origin: String = "IMPLICIT"
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class CachedDependsData(
  val pluginId: String = "",
  val isOptional: Boolean = false,
  val configFile: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class CachedContentModuleDepData(
  val moduleName: String = "",
  val namespace: String = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class CachedModuleData(
  val type: String = "filebased",
  val name: String = "",
  val namespace: String? = null,
  val actualNamespace: String = "",
  val loadingRule: String = "optional",
  val textContent: String? = null,
  val configFile: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class CachedContentDescriptor(
  val services: List<CachedServiceDescriptor> = emptyList(),
  val components: List<CachedComponentConfig> = emptyList(),
  val listeners: List<CachedListenerDescriptor> = emptyList(),
  val extensionPoints: List<CachedExtensionPoint> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class CachedServiceDescriptor(
  val serviceInterface: String? = null,
  val serviceImplementation: String? = null,
  val type: String = "PROJECT",
  val testServiceImplementation: String? = null,
  val headlessImplementation: String? = null,
  val overrides: Boolean? = null,
  val configurationSchemaKey: String? = null,
  val preload: String = "FALSE",
  val client: String? = null,
  val os: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class CachedComponentConfig(
  val interfaceClass: String? = null,
  val implementationClass: String = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class CachedListenerDescriptor(
  val topicName: String = "",
  val className: String = "",
  val type: String = "APPLICATION",
  val activeInTestMode: Boolean = true,
  val activeInHeadlessMode: Boolean = true,
  val os: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class CachedExtensionPoint(
  val extensionPointName: String = "",
  val isDynamic: Boolean = false
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class CachedModuleDescriptorData(
  val name: String = "",
  val moduleDefinition: CachedModuleData = CachedModuleData(),
  val module: CachedPluginData = CachedPluginData()
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class CachedOptionalDescriptorData(
  val dependencyId: String = "",
  val dependencyIsOptional: Boolean = true,
  val dependencyIsModule: Boolean = false,
  val optionalPlugin: CachedPluginData = CachedPluginData(),
  val configurationFilePath: String = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class CachedThemeData(
  val name: String = "",
  val dark: Boolean = false
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class CachedProductDescriptorData(
  val code: String = "",
  val releaseDate: String = "",
  val releaseVersion: Int = 0,
  val eap: Boolean = false,
  val optional: Boolean = false
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class CachedThirdPartyDependency(
  val licenseUrl: String? = null,
  val license: String? = null,
  val url: String? = null,
  val name: String = "",
  val version: String = ""
)
