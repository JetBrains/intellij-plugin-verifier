/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.intellij.beans.ContentModuleDependencyBean
import com.jetbrains.plugin.structure.intellij.beans.PluginBean
import com.jetbrains.plugin.structure.intellij.beans.PluginDependencyBean
import com.jetbrains.plugin.structure.intellij.problems.ElementMissingAttribute
import com.jetbrains.plugin.structure.intellij.problems.UnknownServiceClientValue
import com.jetbrains.plugin.structure.intellij.verifiers.ProblemRegistrar
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.intellij.version.ProductReleaseVersion
import org.jdom2.Document
import org.jdom2.Element
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val LOG = LoggerFactory.getLogger(PluginBeanToIdePluginConverter::class.java)

internal class PluginBeanToIdePluginConverter {
  private val pluginModuleResolver = PluginModuleResolver()

  fun convert(
    bean: PluginBean,
    document: Document,
    parentPlugin: PluginCreator?,
    problemRegistrar: ProblemRegistrar,
    targetPlugin: IdePluginImpl
  ) {
    targetPlugin.apply {
      pluginName = bean.name?.trim()
      pluginId = bean.id?.trim() ?: pluginName
      val idProvider = IdProvider(this, parentPlugin)
      url = bean.url?.trim()
      pluginVersion = if (bean.pluginVersion != null) bean.pluginVersion.trim() else null
      bean.pluginAliases.forEach { pluginId -> addPluginAlias(pluginId) }
      useIdeClassLoader = bean.useIdeaClassLoader == true
      isImplementationDetail = bean.implementationDetail == true

      sinceBuild = bean.readSinceBuild()
      untilBuild = bean.readUntilBuild()

      hasPackagePrefix = bean.packageName != null
      moduleVisibility = bean.readVisibility()

      // dependencies from `<depends>`
      readV1Dependencies(bean)
      // dependencies from `<dependencies>`
      readV2Dependencies(bean, parentPlugin)

      if (pluginModuleResolver.supports(bean)) {
        contentModules += pluginModuleResolver.resolvePluginModules(bean)
      }

      bean.incompatibleWith?.let {
        incompatibleWith += it
      }

      readVendor(bean)
      readProductDescriptor(bean)

      changeNotes = bean.changeNotes
      description = bean.description

      val rootElement = document.rootElement
      readActions(rootElement, this)

      readExtensions(rootElement, this, problemRegistrar)
      readExtensionPoints(rootElement, this, idProvider)

      readListeners(rootElement, "applicationListeners", appContainerDescriptor, problemRegistrar)
      readListeners(rootElement, "projectListeners", projectContainerDescriptor, problemRegistrar)

      readComponents(rootElement, "application-components", appContainerDescriptor)
      readComponents(rootElement, "project-components", projectContainerDescriptor)
      readComponents(rootElement, "module-components", moduleContainerDescriptor)
    }
  }

  private fun PluginBean.readSinceBuild(): IdeVersion? = ideaVersion?.sinceBuild?.let {
    IdeVersion.createIdeVersion(it)
  }

  private fun PluginBean.readUntilBuild(): IdeVersion? {
    val untilBuild = ideaVersion?.untilBuild?.takeIf { it.isNotEmpty() } ?: return null
    val resolvedUntilBuild = if (untilBuild.endsWith(".*")) {
      untilBuild.substringBeforeLast('.') + ".${Int.MAX_VALUE}"
    } else {
      untilBuild
    }
    return IdeVersion.createIdeVersion(resolvedUntilBuild)
  }

  private fun IdePluginImpl.readVendor(bean: PluginBean) {
    bean.vendor?.let { vendorBean ->
      vendor = if (vendorBean.name != null) vendorBean.name.trim { it <= ' ' } else null
      vendorUrl = vendorBean.url
      vendorEmail = vendorBean.email
    }
  }

  private fun IdePluginImpl.readProductDescriptor(bean: PluginBean) {
    bean.productDescriptor?.run {
      productDescriptor = ProductDescriptor(
        code,
        LocalDate.parse(releaseDate, RELEASE_DATE_FORMATTER),
        ProductReleaseVersion.parse(releaseVersion),
        eap == "true",
        optional == "true"
      )
    }
  }

  /**
   * Read dependencies from `<depends>`.
   */
  private fun IdePluginImpl.readV1Dependencies(bean: PluginBean) {
    bean.dependenciesV1.forEach {
      addDepends(DependsPluginDependency(it.dependencyId, it.isOptional, it.configFile))
      // add to the legacy all-aggregating list of dependencies
      dependencies += it.asV1Dependency()
    }
  }

  /**
   * Read dependencies from `<dependencies>`
   */
  private fun IdePluginImpl.readV2Dependencies(bean: PluginBean, parentPlugin: PluginCreator?) {
    bean.contentModuleDependencies.forEach { dep ->
      addContentModuleDependency(ContentModuleDependency(dep.moduleName, dep.resolveNamespace(parentPlugin)))
    }
    dependencies += bean.contentModuleDependencies.map { ModuleV2Dependency(it.moduleName) }
    bean.pluginMainModuleDependencies.forEach {
      addPluginMainModuleDependency(PluginMainModuleDependency(it.dependencyId))
    }
    dependencies += bean.pluginMainModuleDependencies.map { PluginV2Dependency(it.dependencyId) }
  }

  private fun readActions(rootElement: Element, idePlugin: IdePluginImpl) {
    for (actionsRoot in rootElement.getChildren("actions")) {
      idePlugin.actions += actionsRoot.children
    }
  }

  private fun readExtensions(rootElement: Element, idePlugin: IdePluginImpl, problemRegistrar: ProblemRegistrar) {
    for (extensionsRoot in rootElement.getChildren("extensions")) {
      for (extensionElement in extensionsRoot.children) {
        when (val epName = extractEPName(extensionElement)) {
          "com.intellij.applicationService" -> idePlugin.appContainerDescriptor.services += readServiceDescriptor(
            extensionElement,
            epName,
            problemRegistrar
          )

          "com.intellij.projectService" -> idePlugin.projectContainerDescriptor.services += readServiceDescriptor(
            extensionElement,
            epName,
            problemRegistrar
          )

          "com.intellij.moduleService" -> idePlugin.moduleContainerDescriptor.services += readServiceDescriptor(
            extensionElement,
            epName,
            problemRegistrar
          )

          "org.jetbrains.kotlin.supportsKotlinPluginMode" -> {
            idePlugin.addExtension(epName, extensionElement)
            idePlugin.kotlinPluginMode = readKotlinPluginMode(extensionElement)
          }

          else -> idePlugin.addExtension(epName, extensionElement)
        }
      }
    }
  }

  private fun extractEPName(extensionElement: Element): String {
    val point = extensionElement.getAttributeValue("point")
    if (point != null) {
      return point
    }

    val parentNs = extensionElement.parentElement?.getAttributeValue("defaultExtensionNs")
    return if (parentNs != null) {
      parentNs + '.' + extensionElement.name
    } else {
      extensionElement.namespace.uri + '.' + extensionElement.name
    }
  }

  private fun readServiceDescriptor(
    extensionElement: Element,
    epName: String,
    problemRegistrar: ProblemRegistrar
  ): IdePluginContentDescriptor.ServiceDescriptor {
    val serviceInterface = extensionElement.getAttributeValue("serviceInterface")
    val serviceImplementation = extensionElement.getAttributeValue("serviceImplementation")
    val serviceType = IdePluginContentDescriptor.ServiceType.valueOf(
      epName.replace("(Service)|(com.intellij.)".toRegex(), "").toUpperCase()
    )
    val testServiceImplementation = extensionElement.getAttributeValue("testServiceImplementation")
    val headlessImplementation = extensionElement.getAttributeValue("headlessImplementation")
    val configurationSchemaKey = extensionElement.getAttributeValue("configurationSchemaKey")
    val overrides = extensionElement.getAttributeBooleanValue("overrides", false)
    val preload = extensionElement.readServicePreloadMode()
    val client = extensionElement.readServiceClient(problemRegistrar)
    //TODO: add OS extraction
    val os: IdePluginContentDescriptor.Os? = null
    return IdePluginContentDescriptor.ServiceDescriptor(
      serviceInterface,
      serviceImplementation,
      serviceType,
      testServiceImplementation,
      headlessImplementation,
      overrides,
      configurationSchemaKey,
      preload,
      client,
      os
    )
  }

  private fun readExtensionPoints(rootElement: Element, idePlugin: IdePluginImpl, idProvider: IdProvider) {
    for (extensionPointsRoot in rootElement.getChildren("extensionPoints")) {
      for (extensionPoint in extensionPointsRoot.children) {
        val extensionPointName = getExtensionPointName(extensionPoint, idProvider) ?: continue
        val containerDescriptor = when (extensionPoint.getAttributeValue("area")) {
          null -> idePlugin.appContainerDescriptor
          "IDEA_APPLICATION" -> idePlugin.appContainerDescriptor
          "IDEA_PROJECT" -> idePlugin.projectContainerDescriptor
          "IDEA_MODULE" -> idePlugin.moduleContainerDescriptor
          else -> null
        } ?: continue
        val isDynamic = extensionPoint.getAttributeBooleanValue("dynamic", false)
        containerDescriptor.extensionPoints += IdePluginContentDescriptor.ExtensionPoint(extensionPointName, isDynamic)
      }
    }
  }

  private fun getExtensionPointName(extensionPoint: Element, idProvider: IdProvider): String? {
    extensionPoint.getAttributeValue("qualifiedName")?.let { return it }
    val name = extensionPoint.getAttributeValue("name") ?: return null
    val pluginId = idProvider.getId() ?: return null
    return "$pluginId.$name"
  }

  private fun readListeners(
    rootElement: Element,
    listenersName: String,
    containerDescriptor: MutableIdePluginContentDescriptor,
    problemRegistrar: ProblemRegistrar
  ) {
    for (listenersRoot in rootElement.getChildren(listenersName)) {
      for (listener in listenersRoot.children) {
        val className = listener.getAttributeValue("class")
        val topicName = listener.getAttributeValue("topic")
        val isActiveInTestMode = listener.getAttributeBooleanValue("activeInTestMode", true)
        val isActiveInHeadlessMode = listener.getAttributeBooleanValue("activeInHeadlessMode", true)
        val os: IdePluginContentDescriptor.Os? = listener.readOs()
        if (className == null) {
          problemRegistrar.registerProblem(ElementMissingAttribute("listener", "class"))
        }
        if (topicName == null) {
          problemRegistrar.registerProblem(ElementMissingAttribute("listener", "topic"))
        }
        if (className != null && topicName != null) {
          val listenerType =
            IdePluginContentDescriptor.ListenerType.valueOf(listenersName.replace("Listeners", "").toUpperCase())
          containerDescriptor.listeners += IdePluginContentDescriptor.ListenerDescriptor(
            topicName,
            className,
            listenerType,
            isActiveInTestMode,
            isActiveInHeadlessMode,
            os
          )
        }
      }
    }
  }

  private fun readComponents(
    rootElement: Element,
    componentsArea: String,
    containerDescriptor: MutableIdePluginContentDescriptor
  ) {
    for (componentsRoot in rootElement.getChildren(componentsArea)) {
      for (component in componentsRoot.getChildren("component")) {
        val interfaceClass = component.getChild("interface-class")?.text
        val implementationClass = component.getChild("implementation-class")?.text
        if (implementationClass != null) {
          containerDescriptor.components += IdePluginContentDescriptor.ComponentConfig(
            interfaceClass,
            implementationClass
          )
        }
      }
    }
  }

  private fun IdePluginImpl.addExtension(epName: String, extensionElement: Element) {
    extensions.getOrPut(epName) { arrayListOf() }.add(extensionElement)
  }

  private fun Element.getAttributeBooleanValue(attributeName: String, default: Boolean): Boolean {
    return getAttributeValue(attributeName)?.toBoolean() ?: default
  }

  private fun readKotlinPluginMode(extensionElement: Element): KotlinPluginMode {
    val supportsK1 = extensionElement.getAttributeBooleanValue("supportsK1", true)
    val supportsK2 = extensionElement.getAttributeBooleanValue("supportsK2", false)
    return KotlinPluginMode.parse(supportsK1, supportsK2)
  }

  private fun Element.readServicePreloadMode() =
    when (val preload = getAttributeValue("preload")) {
      "true" -> IdePluginContentDescriptor.PreloadMode.TRUE
      "await" -> IdePluginContentDescriptor.PreloadMode.AWAIT
      "notHeadless" -> IdePluginContentDescriptor.PreloadMode.NOT_HEADLESS
      "notLightEdit" -> IdePluginContentDescriptor.PreloadMode.NOT_LIGHT_EDIT
      null -> IdePluginContentDescriptor.PreloadMode.FALSE
      else -> IdePluginContentDescriptor.PreloadMode.FALSE.also {
        LOG.error("Unknown preload mode value '$preload'")
      }
    }

  private fun Element.readServiceClient(problemRegistrar: ProblemRegistrar) =
    when (val client = getAttributeValue("client")) {
      "all" -> IdePluginContentDescriptor.ClientKind.ALL
      "local" -> IdePluginContentDescriptor.ClientKind.LOCAL
      "guest" -> IdePluginContentDescriptor.ClientKind.GUEST
      "controller" -> IdePluginContentDescriptor.ClientKind.CONTROLLER
      "owner" -> IdePluginContentDescriptor.ClientKind.OWNER
      "remote" -> IdePluginContentDescriptor.ClientKind.REMOTE
      "frontend" -> IdePluginContentDescriptor.ClientKind.FRONTEND
      null -> null
      else -> null.also { problemRegistrar.registerProblem(UnsupportedClientAttributeValue(client)) }
    }

  private fun Element.readOs() = when (val os = getAttributeValue("os")) {
    "mac" -> IdePluginContentDescriptor.Os.mac
    "linux" -> IdePluginContentDescriptor.Os.linux
    "windows" -> IdePluginContentDescriptor.Os.windows
    "unix" -> IdePluginContentDescriptor.Os.unix
    "freebsd" -> IdePluginContentDescriptor.Os.freebsd
    null -> null
    else -> null.also { LOG.error("Unknown OS: $os") }
  }

  private fun PluginBean.readVisibility() = when (visibility) {
    "public" -> ModuleVisibility.PUBLIC
    "internal" -> ModuleVisibility.INTERNAL
    else -> ModuleVisibility.PRIVATE
  }

  private fun PluginDependencyBean.asV1Dependency(): PluginV1Dependency {
    return if (isOptional) {
      PluginV1Dependency.Optional(dependencyId)
    } else {
      PluginV1Dependency.Mandatory(dependencyId)
    }
  }

  private fun ContentModuleDependencyBean.resolveNamespace(parentPlugin: PluginCreator?): String {
    return namespace
      ?: parentPlugin?.plugin?.contentModules
        ?.find { it.name == moduleName }
        ?.actualNamespace ?: "jetbrains"
  }

  private class IdProvider(private val plugin: Plugin, val parentPlugin: PluginCreator?) {
    fun getId(): String? = plugin.pluginId ?: parentPlugin?.pluginId
  }

  /**
   * Maps to [UnknownServiceClientValue] but without plugin descriptor reference.
   */
  internal class UnsupportedClientAttributeValue(val unsupportedValue: String) : PluginProblem() {
    override val level = Level.WARNING
    override val message = "Unsupported value of attribute 'client': [$unsupportedValue]"
  }

  private companion object {
    val RELEASE_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
  }
}
