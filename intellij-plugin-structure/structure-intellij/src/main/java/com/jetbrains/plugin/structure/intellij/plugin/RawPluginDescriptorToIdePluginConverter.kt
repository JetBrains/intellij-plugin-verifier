/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.intellij.platform.pluginSystem.parser.impl.RawPluginDescriptor
import com.intellij.platform.pluginSystem.parser.impl.ScopedElementsContainer
import com.intellij.platform.pluginSystem.parser.impl.elements.ClientKindValue
import com.intellij.platform.pluginSystem.parser.impl.elements.ComponentElement
import com.intellij.platform.pluginSystem.parser.impl.elements.ContentModuleElement
import com.intellij.platform.pluginSystem.parser.impl.elements.DependenciesElement
import com.intellij.platform.pluginSystem.parser.impl.elements.ExtensionPointElement
import com.intellij.platform.pluginSystem.parser.impl.elements.ModuleLoadingRuleValue
import com.intellij.platform.pluginSystem.parser.impl.elements.ModuleVisibilityValue
import com.intellij.platform.pluginSystem.parser.impl.elements.OSValue
import com.intellij.platform.pluginSystem.parser.impl.elements.PreloadModeValue
import com.intellij.platform.pluginSystem.parser.impl.elements.ServiceElement
import com.intellij.util.xml.dom.XmlElement
import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.intellij.verifiers.ProblemRegistrar
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.intellij.version.ProductReleaseVersion
import org.jdom2.Document
import org.jdom2.Element
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger(RawPluginDescriptorToIdePluginConverter::class.java)

/**
 * POC counterpart to [PluginBeanToIdePluginConverter]: maps a [RawPluginDescriptor] (produced by
 * [PlatformPluginDescriptorParser], JetBrains' own `plugin-system-parser-impl`) onto the SAME
 * [IdePluginImpl] domain object that the JAXB path produces, so every downstream consumer of
 * [IdePlugin] keeps working unchanged regardless of which parser ran.
 *
 * Compared to [PluginBeanToIdePluginConverter], this converter is structurally simpler in one
 * respect: [RawPluginDescriptor] already separates services/components/listeners/extensionPoints
 * per scope (app/project/module) via [RawPluginDescriptor.appElementsContainer] and friends, and
 * already fully qualifies generic `<extensions>` entries by EP name. The old converter has to do
 * both of those by hand (string-matching known service EP names, walking the raw JDOM tree) - see
 * [PluginBeanToIdePluginConverter.readExtensions] / [PluginBeanToIdePluginConverter.extractEPName].
 * One thing it does NOT do that the old converter's [PluginBeanToIdePluginConverter.getExtensionPointName]
 * does: compute a `pluginId.name` fallback when only a bare `name` (no `qualifiedName`) is given for
 * an `<extensionPoints>` entry - [qualify] below reimplements that.
 *
 * Known, accepted gaps for this POC (see also PlatformPluginDescriptorParser's class doc):
 *  - `<product-descriptor eap="...">` has no home on [RawPluginDescriptor] at all - hardcoded to
 *    `false` below. See [readProductDescriptor].
 *  - [ServiceElement.open] has no field on [IdePluginContentDescriptor.ServiceDescriptor] - dropped.
 *  - [ComponentElement.headlessImplementationClass]/`loadForDefaultProject`/`overrides`/`options`
 *    likewise have no home on [IdePluginContentDescriptor.ComponentConfig] - dropped.
 *  - `ExtensionElement.element` never carries the original XML tag's local name: the upstream parser
 *    calls `readXmlAsModel(reader, rootName = null, ...)` for generic extensions (vs. `readActions`,
 *    which passes the real `elementName`), and [ExtensionElement] has no field of its own for it
 *    either. So `element.name` is blank for essentially every generic extension (also `element`
 *    itself can be null for extensions with zero attributes/children, a separate memory
 *    optimization). [toJdomElement] always falls back to a name derived from the EP's last segment
 *    in both cases - see its own doc.
 */
internal class RawPluginDescriptorToIdePluginConverter {

  fun convert(
    raw: RawPluginDescriptor,
    document: Document,
    parentPlugin: PluginCreator?,
    problemRegistrar: ProblemRegistrar,
    targetPlugin: IdePluginImpl
  ) {
    targetPlugin.apply {
      pluginName = raw.name?.trim()
      pluginId = raw.id?.trim() ?: pluginName
      val idProvider = IdProvider(this, parentPlugin)
      url = raw.url?.trim()
      pluginVersion = raw.version?.trim()
      raw.pluginAliases.forEach { alias -> addPluginAlias(alias) }
      useIdeClassLoader = raw.isUseIdeaClassLoader
      isImplementationDetail = raw.isImplementationDetail

      sinceBuild = raw.sinceBuild?.let { IdeVersion.createIdeVersion(it) }
      untilBuild = readUntilBuild(raw)

      hasPackagePrefix = raw.`package` != null
      moduleVisibility = raw.moduleVisibility.toDomainVisibility()

      readV1Dependencies(raw)
      readV2Dependencies(raw, parentPlugin)

      contentModules += raw.contentModules.map { it.toModule(pluginId) }

      incompatibleWith += raw.incompatibleWith

      readVendor(raw)
      readProductDescriptor(raw)

      changeNotes = raw.changeNotes
      description = raw.description

      actions += raw.actions.map { it.element.toJdomElement() }

      readExtensions(raw, this, problemRegistrar)

      raw.appElementsContainer.copyInto(appContainerDescriptor, IdePluginContentDescriptor.ServiceType.APPLICATION, IdePluginContentDescriptor.ListenerType.APPLICATION, idProvider)
      raw.projectElementsContainer.copyInto(projectContainerDescriptor, IdePluginContentDescriptor.ServiceType.PROJECT, IdePluginContentDescriptor.ListenerType.PROJECT, idProvider)
      // There is no <moduleListeners> tag in plugin.xml, so moduleElementsContainer.listeners is
      // always empty upstream too - pass listenerType = null and skip listener copying for this scope.
      raw.moduleElementsContainer.copyInto(moduleContainerDescriptor, IdePluginContentDescriptor.ServiceType.MODULE, null, idProvider)
    }
  }

  private fun readUntilBuild(raw: RawPluginDescriptor): IdeVersion? {
    val untilBuild = raw.untilBuild?.takeIf { it.isNotEmpty() } ?: return null
    val resolvedUntilBuild = if (untilBuild.endsWith(".*")) {
      untilBuild.substringBeforeLast('.') + ".${Int.MAX_VALUE}"
    } else {
      untilBuild
    }
    return IdeVersion.createIdeVersion(resolvedUntilBuild)
  }

  private fun IdePluginImpl.readVendor(raw: RawPluginDescriptor) {
    vendor = raw.vendor?.trim { it <= ' ' }
    vendorUrl = raw.vendorUrl
    vendorEmail = raw.vendorEmail
  }

  /**
   * KNOWN GAP: no `eap` field exists on [RawPluginDescriptor] (confirmed against
   * intellij-community's `XmlReader.readProduct()`, which only reads code/release-date/
   * release-version/`optional` - `optional` maps to the top-level [RawPluginDescriptor.isLicenseOptional],
   * not a nested field). A plugin declaring `<product-descriptor eap="true">` loses that flag here.
   */
  private fun IdePluginImpl.readProductDescriptor(raw: RawPluginDescriptor) {
    val code = raw.productCode
    val releaseDate = raw.releaseDate
    if (code != null && releaseDate != null) {
      productDescriptor = ProductDescriptor(
        code,
        releaseDate,
        ProductReleaseVersion(raw.releaseVersion),
        eap = false, // see class/method doc - not modeled upstream, cannot be recovered here
        optional = raw.isLicenseOptional
      )
    }
  }

  /** Read dependencies from `<depends>`. */
  private fun IdePluginImpl.readV1Dependencies(raw: RawPluginDescriptor) {
    raw.depends.forEach {
      val pluginId = it.pluginId ?: return@forEach
      addDepends(DependsPluginDependency(pluginId, it.isOptional, it.configFile))
      dependencies += if (it.isOptional) PluginV1Dependency.Optional(pluginId) else PluginV1Dependency.Mandatory(pluginId)
    }
  }

  /** Read dependencies from `<dependencies>`. */
  private fun IdePluginImpl.readV2Dependencies(raw: RawPluginDescriptor, parentPlugin: PluginCreator?) {
    raw.dependencies.forEach { dependency ->
      when (dependency) {
        is DependenciesElement.ModuleDependency -> {
          addContentModuleDependency(ContentModuleDependency(dependency.moduleName, dependency.resolveNamespace(parentPlugin)))
          dependencies += ModuleV2Dependency(dependency.moduleName)
        }
        is DependenciesElement.PluginDependency -> {
          addPluginMainModuleDependency(PluginMainModuleDependency(dependency.pluginId))
          dependencies += PluginV2Dependency(dependency.pluginId)
        }
      }
    }
  }

  private fun DependenciesElement.ModuleDependency.resolveNamespace(parentPlugin: PluginCreator?): String {
    return namespace
      ?: parentPlugin?.plugin?.contentModules
        ?.find { it.name == moduleName }
        ?.actualNamespace ?: "jetbrains"
  }

  private fun ContentModuleElement.toModule(pluginId: String?): Module {
    // Mirrors PluginModuleResolver's synthetic-namespace convention for private modules.
    val actualNamespace = namespace ?: "${pluginId}_\$implicit"
    val rule = loadingRule.toDomainLoadingRule()
    // Blank content means file-based, not "an inline module whose descriptor happens to be empty".
    // Both `<module name="x"/>` and `<module name="x"></module>` are references to a separate
    // descriptor file, but the library reports the latter as an EMPTY `embeddedDescriptorContent`
    // rather than a null one, so a plain null check would classify it as inline and then hand an
    // empty string to a JDOM parse. This mirrors PluginModuleResolver's `isNullOrBlank()` test,
    // which is what the JAXB path uses to draw the same line.
    val embedded = embeddedDescriptorContent?.let { String(it) }
    return if (!embedded.isNullOrBlank()) {
      Module.InlineModule(name, namespace, actualNamespace, rule, embedded)
    } else {
      // Same file-name convention as PluginModuleResolver.resolvePluginModules.
      val configFile = "../${name.replace("/", ".")}.xml"
      Module.FileBasedModule(name, namespace, actualNamespace, rule, configFile)
    }
  }

  private fun ModuleLoadingRuleValue.toDomainLoadingRule(): ModuleLoadingRule = when (this) {
    ModuleLoadingRuleValue.REQUIRED -> ModuleLoadingRule.REQUIRED
    ModuleLoadingRuleValue.EMBEDDED -> ModuleLoadingRule.EMBEDDED
    ModuleLoadingRuleValue.OPTIONAL -> ModuleLoadingRule.OPTIONAL
    ModuleLoadingRuleValue.ON_DEMAND -> ModuleLoadingRule.ON_DEMAND
  }

  private fun ModuleVisibilityValue.toDomainVisibility(): ModuleVisibility = when (this) {
    ModuleVisibilityValue.PRIVATE -> ModuleVisibility.PRIVATE
    ModuleVisibilityValue.INTERNAL -> ModuleVisibility.INTERNAL
    ModuleVisibilityValue.PUBLIC -> ModuleVisibility.PUBLIC
  }

  /**
   * `raw.extensions` (unlike the old JDOM tree walk) can never contain service entries - the
   * upstream parser (`XmlReader.readExtensions`) diverts `com.intellij.{application,project,module}Service`
   * into the scoped containers *before* ever calling `builder.addExtension`. So, unlike
   * [PluginBeanToIdePluginConverter.readExtensions], there's no need to special-case those EP names
   * here; the only remaining special case is `supportsKotlinPluginMode`, kept for parity.
   */
  private fun readExtensions(raw: RawPluginDescriptor, idePlugin: IdePluginImpl, problemRegistrar: ProblemRegistrar) {
    raw.extensions.forEach { (epName, extensionElements) ->
      extensionElements.forEach { ext ->
        val jdomElement = ext.element.toJdomElement(fallbackTagName = epName.substringAfterLast('.'))
        if (epName == "org.jetbrains.kotlin.supportsKotlinPluginMode") {
          idePlugin.addExtension(epName, jdomElement)
          idePlugin.kotlinPluginMode = readKotlinPluginMode(jdomElement)
        } else {
          idePlugin.addExtension(epName, jdomElement)
        }
      }
    }
  }

  private fun IdePluginImpl.addExtension(epName: String, extensionElement: Element) {
    extensions.getOrPut(epName) { arrayListOf() }.add(extensionElement)
  }

  private fun readKotlinPluginMode(extensionElement: Element): KotlinPluginMode {
    val supportsK1 = extensionElement.getAttributeValue("supportsK1")?.toBoolean() ?: true
    val supportsK2 = extensionElement.getAttributeValue("supportsK2")?.toBoolean() ?: false
    return KotlinPluginMode.parse(supportsK1, supportsK2)
  }

  private fun ScopedElementsContainer.copyInto(
    target: MutableIdePluginContentDescriptor,
    serviceType: IdePluginContentDescriptor.ServiceType,
    listenerType: IdePluginContentDescriptor.ListenerType?,
    idProvider: IdProvider
  ) {
    target.services += services.map { it.toServiceDescriptor(serviceType) }

    // Old readComponents only kept a component when implementationClass != null - same gate here.
    target.components += components.mapNotNull { component ->
      component.implementationClass?.let { impl ->
        IdePluginContentDescriptor.ComponentConfig(component.interfaceClass, impl)
      }
    }

    if (listenerType != null) {
      target.listeners += listeners.map { listener ->
        IdePluginContentDescriptor.ListenerDescriptor(
          listener.topicClassName,
          listener.listenerClassName,
          listenerType,
          listener.activeInTestMode,
          listener.activeInHeadlessMode,
          listener.os.toDomainOs()
        )
      }
    }

    target.extensionPoints += extensionPoints.mapNotNull { ep -> ep.qualify(idProvider)?.let {
      IdePluginContentDescriptor.ExtensionPoint(it, ep.isDynamic)
    } }
  }

  private fun ServiceElement.toServiceDescriptor(type: IdePluginContentDescriptor.ServiceType) =
    IdePluginContentDescriptor.ServiceDescriptor(
      serviceInterface,
      serviceImplementation,
      type,
      testServiceImplementation,
      headlessImplementation,
      overrides,
      configurationSchemaKey,
      preload.toDomainPreloadMode(),
      client.toDomainClientKind(),
      os.toDomainOs()
      // `open` is dropped here - see class doc.
    )

  /** Mirrors [PluginBeanToIdePluginConverter.getExtensionPointName]'s `pluginId.name` fallback. */
  private fun ExtensionPointElement.qualify(idProvider: IdProvider): String? {
    qualifiedName?.let { return it }
    val n = name ?: return null
    val pluginId = idProvider.getId() ?: return null
    return "$pluginId.$n"
  }

  private fun PreloadModeValue.toDomainPreloadMode(): IdePluginContentDescriptor.PreloadMode = when (this) {
    PreloadModeValue.TRUE -> IdePluginContentDescriptor.PreloadMode.TRUE
    PreloadModeValue.FALSE -> IdePluginContentDescriptor.PreloadMode.FALSE
    PreloadModeValue.AWAIT -> IdePluginContentDescriptor.PreloadMode.AWAIT
    PreloadModeValue.NOT_HEADLESS -> IdePluginContentDescriptor.PreloadMode.NOT_HEADLESS
    PreloadModeValue.NOT_LIGHT_EDIT -> IdePluginContentDescriptor.PreloadMode.NOT_LIGHT_EDIT
  }

  private fun ClientKindValue?.toDomainClientKind(): IdePluginContentDescriptor.ClientKind? = when (this) {
    null -> null
    ClientKindValue.ALL -> IdePluginContentDescriptor.ClientKind.ALL
    ClientKindValue.GUEST -> IdePluginContentDescriptor.ClientKind.GUEST
    ClientKindValue.LOCAL -> IdePluginContentDescriptor.ClientKind.LOCAL
    ClientKindValue.CONTROLLER -> IdePluginContentDescriptor.ClientKind.CONTROLLER
    ClientKindValue.OWNER -> IdePluginContentDescriptor.ClientKind.OWNER
    ClientKindValue.REMOTE -> IdePluginContentDescriptor.ClientKind.REMOTE
    ClientKindValue.FRONTEND -> IdePluginContentDescriptor.ClientKind.FRONTEND
  }

  private fun OSValue?.toDomainOs(): IdePluginContentDescriptor.Os? = when (this) {
    null -> null
    OSValue.MAC -> IdePluginContentDescriptor.Os.mac
    OSValue.LINUX -> IdePluginContentDescriptor.Os.linux
    OSValue.WINDOWS -> IdePluginContentDescriptor.Os.windows
    OSValue.UNIX -> IdePluginContentDescriptor.Os.unix
    OSValue.FREEBSD -> IdePluginContentDescriptor.Os.freebsd
  }

  /**
   * Converts the library's lightweight [XmlElement] (from `util-xml-dom`) into the JDOM [Element]
   * that the rest of this codebase (and [IdePlugin]'s public surface) is built around.
   *
   * For GENERIC extensions, `XmlReader.readExtensions` calls `readXmlAsModel(reader, rootName =
   * null, interner)` - passing `rootName = null` - whereas `readActions` passes the real
   * `elementName`. [ExtensionElement] has no field of its own for the original XML tag name (unlike
   * [ActionElement], which doesn't need one because it stores a dedicated `name: ActionElementName`
   * enum), so the library leaves the wrapper `XmlElement`'s own `name` blank in that case - it's
   * simply not part of the extension model, only its attributes/children/content are. So
   * `element.name` is blank for essentially EVERY generic extension, not just an edge case - always
   * fall back to [fallbackTagName] whenever `name` is blank, not only when `element` itself is null.
   */
  private fun XmlElement?.toJdomElement(fallbackTagName: String = "extension"): Element {
    if (this == null) {
      LOG.debug("Null extension element (no attributes/children); using placeholder tag '$fallbackTagName'")
      return Element(fallbackTagName)
    }
    val tagName = name.ifEmpty { fallbackTagName }
    val jdomElement = Element(tagName)
    attributes.forEach { (key, value) -> jdomElement.setAttribute(key, value) }
    content?.let { jdomElement.text = it }
    children.forEach { jdomElement.addContent(it.toJdomElement()) }
    return jdomElement
  }

  private class IdProvider(private val plugin: Plugin, val parentPlugin: PluginCreator?) {
    fun getId(): String? = plugin.pluginId ?: parentPlugin?.pluginId
  }
}

/**
 * [RawPluginDescriptor]-typed counterpart to [newInvalidPlugin] (which is [PluginBean]-typed).
 * Kept in this file, not InvalidPlugin.kt, to keep all `plugin-system-parser-impl`-facing code in
 * one place (see PlatformPluginDescriptorParser's class doc on why that matters for `@ApiStatus.Internal`
 * churn - though for a JetBrains-internal consumer like this project, that's routine maintenance,
 * not a special risk).
 */
internal fun newInvalidPlugin(raw: RawPluginDescriptor, document: Document): InvalidPlugin {
  return InvalidPlugin(document).apply {
    pluginId = raw.id?.trim()
    pluginName = raw.name?.trim()
    vendor = raw.vendor?.trim()
    vendorUrl = raw.vendorUrl
    vendorEmail = raw.vendorEmail
  }
}
