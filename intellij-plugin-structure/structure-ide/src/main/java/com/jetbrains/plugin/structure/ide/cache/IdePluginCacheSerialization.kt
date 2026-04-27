/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.cache

import com.jetbrains.plugin.structure.base.plugin.ThirdPartyDependency
import com.jetbrains.plugin.structure.intellij.plugin.Classpath
import com.jetbrains.plugin.structure.intellij.plugin.ClasspathEntry
import com.jetbrains.plugin.structure.intellij.plugin.ClasspathOrigin
import com.jetbrains.plugin.structure.intellij.plugin.ContentModuleDependency
import com.jetbrains.plugin.structure.intellij.plugin.DependsPluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginContentDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import com.jetbrains.plugin.structure.intellij.plugin.IdeTheme
import com.jetbrains.plugin.structure.intellij.plugin.KotlinPluginMode
import com.jetbrains.plugin.structure.intellij.plugin.Module
import com.jetbrains.plugin.structure.intellij.plugin.ModuleDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.ModuleLoadingRule
import com.jetbrains.plugin.structure.intellij.plugin.ModuleVisibility
import com.jetbrains.plugin.structure.intellij.plugin.MutableIdePluginContentDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.OptionalPluginDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.PluginMainModuleDependency
import com.jetbrains.plugin.structure.intellij.plugin.ProductDescriptor
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.intellij.version.ProductReleaseVersion
import org.jdom2.input.SAXBuilder
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import java.io.StringReader
import java.nio.file.Path
import java.time.LocalDate

internal fun IdePlugin.toCachedPluginData(idePath: Path, outputter: XMLOutputter): CachedPluginData {
  return CachedPluginData(
    pluginId = pluginId,
    pluginName = pluginName,
    pluginVersion = pluginVersion,
    sinceBuild = sinceBuild?.asString(),
    untilBuild = untilBuild?.asString(),
    originalFile = originalFile?.relativizeAgainst(idePath),
    classpath = classpath.entries.map { CachedClasspathEntry(it.path.relativizeAgainst(idePath), it.origin.name) },
    dependsList = dependsList.map { CachedDependsData(it.pluginId, it.isOptional, it.configFile) },
    contentModuleDependencies = contentModuleDependencies.map { CachedContentModuleDepData(it.moduleName, it.namespace) },
    pluginMainModuleDependencies = pluginMainModuleDependencies.map { it.pluginId },
    incompatibleWith = incompatibleWith.toList(),
    pluginAliases = pluginAliases.toSet(),
    contentModules = contentModules.map { it.toCachedModuleData() },
    isImplementationDetail = isImplementationDetail,
    useIdeClassLoader = useIdeClassLoader,
    hasPackagePrefix = hasPackagePrefix,
    moduleVisibility = moduleVisibility.name,
    kotlinK1Compatible = kotlinPluginMode.isK1Compatible,
    kotlinK2Compatible = kotlinPluginMode.isK2Compatible,
    hasDotNetPart = hasDotNetPart,
    underlyingDocumentXml = outputter.outputString(underlyingDocument),
    extensionsXml = extensions.mapValues { (_, elements) -> elements.map { outputter.outputString(it) } },
    actionsXml = if (this is IdePluginImpl) actions.map { outputter.outputString(it) } else emptyList(),
    appContainerDescriptor = appContainerDescriptor.toCachedContentDescriptor(),
    projectContainerDescriptor = projectContainerDescriptor.toCachedContentDescriptor(),
    moduleContainerDescriptor = moduleContainerDescriptor.toCachedContentDescriptor(),
    modulesDescriptors = modulesDescriptors.map { it.toCachedModuleDescriptorData(idePath, outputter) },
    optionalDescriptors = optionalDescriptors.map { it.toCachedOptionalDescriptorData(idePath, outputter) },
    declaredThemes = declaredThemes.map { CachedThemeData(it.name, it.dark) },
    productDescriptor = productDescriptor?.toCachedProductDescriptorData(),
    thirdPartyDependencies = thirdPartyDependencies.map { it.toCachedThirdPartyDependency() },
    vendor = vendor,
    vendorEmail = vendorEmail,
    vendorUrl = vendorUrl,
    description = description,
    changeNotes = changeNotes,
    url = url
  )
}

internal fun CachedPluginData.toIdePluginImpl(idePath: Path, builder: SAXBuilder): IdePluginImpl {
  val cached = this
  return IdePluginImpl().apply {
    pluginId = cached.pluginId
    pluginName = cached.pluginName
    pluginVersion = cached.pluginVersion
    sinceBuild = cached.sinceBuild?.let { IdeVersion.createIdeVersionIfValid(it) }
    untilBuild = cached.untilBuild?.let { IdeVersion.createIdeVersionIfValid(it) }
    originalFile = cached.originalFile?.toAbsolutePath(idePath)
    classpath = Classpath.ofEntries(cached.classpath.map {
      ClasspathEntry(it.path.toAbsolutePath(idePath), ClasspathOrigin.valueOf(it.origin))
    })
    cached.dependsList.forEach { addDepends(DependsPluginDependency(it.pluginId, it.isOptional, it.configFile)) }
    cached.contentModuleDependencies.forEach { addContentModuleDependency(ContentModuleDependency(it.moduleName, it.namespace)) }
    cached.pluginMainModuleDependencies.forEach { addPluginMainModuleDependency(PluginMainModuleDependency(it)) }
    incompatibleWith.addAll(cached.incompatibleWith)
    cached.pluginAliases.forEach { addPluginAlias(it) }
    contentModules.addAll(cached.contentModules.map { it.toModule() })
    isImplementationDetail = cached.isImplementationDetail
    useIdeClassLoader = cached.useIdeClassLoader
    hasPackagePrefix = cached.hasPackagePrefix
    moduleVisibility = ModuleVisibility.valueOf(cached.moduleVisibility)
    kotlinPluginMode = KotlinPluginMode.parse(cached.kotlinK1Compatible, cached.kotlinK2Compatible)
    hasDotNetPart = cached.hasDotNetPart
    if (cached.underlyingDocumentXml.isNotEmpty()) {
      underlyingDocument = builder.build(StringReader(cached.underlyingDocumentXml))
    }
    cached.extensionsXml.forEach { (epName, xmlList) ->
      extensions[epName] = xmlList.mapTo(mutableListOf()) { builder.build(StringReader(it)).rootElement }
    }
    actions.addAll(cached.actionsXml.map { builder.build(StringReader(it)).rootElement })
    cached.appContainerDescriptor.populateInto(appContainerDescriptor)
    cached.projectContainerDescriptor.populateInto(projectContainerDescriptor)
    cached.moduleContainerDescriptor.populateInto(moduleContainerDescriptor)
    modulesDescriptors.addAll(cached.modulesDescriptors.map { it.toModuleDescriptor(idePath, builder) })
    optionalDescriptors.addAll(cached.optionalDescriptors.map { it.toOptionalPluginDescriptor(idePath, builder) })
    declaredThemes.addAll(cached.declaredThemes.map { IdeTheme(it.name, it.dark) })
    productDescriptor = cached.productDescriptor?.toProductDescriptor()
    thirdPartyDependencies = cached.thirdPartyDependencies.map { it.toThirdPartyDependency() }
    vendor = cached.vendor
    vendorEmail = cached.vendorEmail
    vendorUrl = cached.vendorUrl
    description = cached.description
    changeNotes = cached.changeNotes
    url = cached.url
    @Suppress("DEPRECATION")
    cached.dependsList.forEach { dep ->
      dependencies.add(DependsPluginDependency(dep.pluginId, dep.isOptional, dep.configFile).asPluginDependency())
    }
  }
}

// --- Module ---

private fun Module.toCachedModuleData(): CachedModuleData = when (this) {
  is Module.InlineModule -> CachedModuleData(
    type = "inline",
    name = name,
    namespace = namespace,
    actualNamespace = actualNamespace,
    loadingRule = loadingRule.id,
    textContent = textContent
  )
  is Module.FileBasedModule -> CachedModuleData(
    type = "filebased",
    name = name,
    namespace = namespace,
    actualNamespace = actualNamespace,
    loadingRule = loadingRule.id,
    configFile = configFile
  )
}

private fun CachedModuleData.toModule(): Module = when (type) {
  "inline" -> Module.InlineModule(
    name = name,
    namespace = namespace,
    actualNamespace = actualNamespace,
    loadingRule = ModuleLoadingRule.create(loadingRule),
    textContent = textContent ?: ""
  )
  else -> Module.FileBasedModule(
    name = name,
    namespace = namespace,
    actualNamespace = actualNamespace,
    loadingRule = ModuleLoadingRule.create(loadingRule),
    configFile = configFile ?: ""
  )
}

// --- ModuleDescriptor ---

private fun ModuleDescriptor.toCachedModuleDescriptorData(idePath: Path, outputter: XMLOutputter) =
  CachedModuleDescriptorData(
    name = name,
    moduleDefinition = moduleDefinition.toCachedModuleData(),
    module = module.toCachedPluginData(idePath, outputter)
  )

private fun CachedModuleDescriptorData.toModuleDescriptor(idePath: Path, builder: SAXBuilder) =
  ModuleDescriptor.of(
    module = module.toIdePluginImpl(idePath, builder),
    moduleDefinition = moduleDefinition.toModule()
  )

// --- OptionalPluginDescriptor ---

private fun OptionalPluginDescriptor.toCachedOptionalDescriptorData(idePath: Path, outputter: XMLOutputter) =
  CachedOptionalDescriptorData(
    dependencyId = dependency.id,
    dependencyIsOptional = dependency.isOptional,
    dependencyIsModule = dependency.isModule,
    optionalPlugin = optionalPlugin.toCachedPluginData(idePath, outputter),
    configurationFilePath = configurationFilePath
  )

private fun CachedOptionalDescriptorData.toOptionalPluginDescriptor(idePath: Path, builder: SAXBuilder): OptionalPluginDescriptor {
  val dep = object : com.jetbrains.plugin.structure.intellij.plugin.PluginDependency {
    override val id = dependencyId
    override val isOptional = dependencyIsOptional
    override val isModule = dependencyIsModule
    override fun asOptional() = this
  }
  return OptionalPluginDescriptor(dep, optionalPlugin.toIdePluginImpl(idePath, builder), configurationFilePath)
}

// --- IdePluginContentDescriptor ---

private fun IdePluginContentDescriptor.toCachedContentDescriptor() = CachedContentDescriptor(
  services = services.map { it.toCachedServiceDescriptor() },
  components = components.map { CachedComponentConfig(it.interfaceClass, it.implementationClass) },
  listeners = listeners.map { it.toCachedListenerDescriptor() },
  extensionPoints = extensionPoints.map { CachedExtensionPoint(it.extensionPointName, it.isDynamic) }
)

private fun CachedContentDescriptor.populateInto(descriptor: MutableIdePluginContentDescriptor) {
  descriptor.services.addAll(services.map { it.toServiceDescriptor() })
  descriptor.components.addAll(components.map { IdePluginContentDescriptor.ComponentConfig(it.interfaceClass, it.implementationClass) })
  descriptor.listeners.addAll(listeners.map { it.toListenerDescriptor() })
  descriptor.extensionPoints.addAll(extensionPoints.map { IdePluginContentDescriptor.ExtensionPoint(it.extensionPointName, it.isDynamic) })
}

private fun IdePluginContentDescriptor.ServiceDescriptor.toCachedServiceDescriptor() = CachedServiceDescriptor(
  serviceInterface = serviceInterface,
  serviceImplementation = serviceImplementation,
  type = type.name,
  testServiceImplementation = testServiceImplementation,
  headlessImplementation = headlessImplementation,
  overrides = overrides,
  configurationSchemaKey = configurationSchemaKey,
  preload = preload.name,
  client = client?.name,
  os = os?.name
)

private fun CachedServiceDescriptor.toServiceDescriptor() = IdePluginContentDescriptor.ServiceDescriptor(
  serviceInterface = serviceInterface,
  serviceImplementation = serviceImplementation,
  type = IdePluginContentDescriptor.ServiceType.valueOf(type),
  testServiceImplementation = testServiceImplementation,
  headlessImplementation = headlessImplementation,
  overrides = overrides,
  configurationSchemaKey = configurationSchemaKey,
  preload = IdePluginContentDescriptor.PreloadMode.valueOf(preload),
  client = client?.let { IdePluginContentDescriptor.ClientKind.valueOf(it) },
  os = os?.let { IdePluginContentDescriptor.Os.valueOf(it) }
)

private fun IdePluginContentDescriptor.ListenerDescriptor.toCachedListenerDescriptor() = CachedListenerDescriptor(
  topicName = topicName,
  className = className,
  type = type.name,
  activeInTestMode = activeInTestMode,
  activeInHeadlessMode = activeInHeadlessMode,
  os = os?.name
)

private fun CachedListenerDescriptor.toListenerDescriptor() = IdePluginContentDescriptor.ListenerDescriptor(
  topicName = topicName,
  className = className,
  type = IdePluginContentDescriptor.ListenerType.valueOf(type),
  activeInTestMode = activeInTestMode,
  activeInHeadlessMode = activeInHeadlessMode,
  os = os?.let { IdePluginContentDescriptor.Os.valueOf(it) }
)

// --- ProductDescriptor ---

private fun ProductDescriptor.toCachedProductDescriptorData() = CachedProductDescriptorData(
  code = code,
  releaseDate = releaseDate.toString(),
  releaseVersion = version.value,
  eap = eap,
  optional = optional
)

private fun CachedProductDescriptorData.toProductDescriptor() = ProductDescriptor(
  code = code,
  releaseDate = LocalDate.parse(releaseDate),
  version = ProductReleaseVersion(releaseVersion),
  eap = eap,
  optional = optional
)

// --- ThirdPartyDependency ---

private fun ThirdPartyDependency.toCachedThirdPartyDependency() = CachedThirdPartyDependency(
  licenseUrl = licenseUrl,
  license = license,
  url = url,
  name = name,
  version = version
)

private fun CachedThirdPartyDependency.toThirdPartyDependency() = ThirdPartyDependency(
  licenseUrl = licenseUrl,
  license = license,
  url = url,
  name = name,
  version = version
)

// --- Path helpers ---

private fun Path.relativizeAgainst(base: Path): String = try {
  base.relativize(this).toString()
} catch (_: IllegalArgumentException) {
  toString()
}

private fun String.toAbsolutePath(base: Path): Path {
  val p = Path.of(this)
  return if (p.isAbsolute) p else base.resolve(p)
}

// --- XMLOutputter factory used by the cache ---

internal fun newXmlOutputter(): XMLOutputter = XMLOutputter(Format.getRawFormat())
internal fun newSaxBuilder(): SAXBuilder = SAXBuilder()
