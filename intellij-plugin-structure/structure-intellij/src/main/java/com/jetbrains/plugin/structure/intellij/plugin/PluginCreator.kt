/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.base.plugin.ThirdPartyDependency
import com.jetbrains.plugin.structure.base.problems.MAX_NAME_LENGTH
import com.jetbrains.plugin.structure.base.problems.NotBoolean
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PluginProblem.Level.ERROR
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.problems.TooLongPropertyValue
import com.jetbrains.plugin.structure.base.problems.UnableToReadDescriptor
import com.jetbrains.plugin.structure.base.problems.VendorCannotBeEmpty
import com.jetbrains.plugin.structure.base.telemetry.MutablePluginTelemetry
import com.jetbrains.plugin.structure.base.telemetry.PluginTelemetry
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.intellij.beans.IdeaVersionBean
import com.jetbrains.plugin.structure.intellij.beans.PluginBean
import com.jetbrains.plugin.structure.intellij.beans.PluginDependencyBean
import com.jetbrains.plugin.structure.intellij.beans.PluginVendorBean
import com.jetbrains.plugin.structure.intellij.beans.ProductDescriptorBean
import com.jetbrains.plugin.structure.intellij.extractor.PluginBeanExtractor
import com.jetbrains.plugin.structure.intellij.problems.*
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.verifiers.K2IdeModeCompatibilityVerifier
import com.jetbrains.plugin.structure.intellij.verifiers.PluginIdVerifier
import com.jetbrains.plugin.structure.intellij.verifiers.PluginUntilBuildVerifier
import com.jetbrains.plugin.structure.intellij.verifiers.ProductReleaseVersionVerifier
import com.jetbrains.plugin.structure.intellij.verifiers.ReusedDescriptorVerifier
import com.jetbrains.plugin.structure.intellij.verifiers.ServiceExtensionPointPreloadVerifier
import com.jetbrains.plugin.structure.intellij.verifiers.StatusBarWidgetFactoryExtensionPointVerifier
import com.jetbrains.plugin.structure.intellij.verifiers.verifyNewlines
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.intellij.version.ProductReleaseVersion
import com.jetbrains.plugin.structure.intellij.xinclude.XIncluder
import com.jetbrains.plugin.structure.intellij.xinclude.XIncluderException
import org.jdom2.Document
import org.jdom2.Element
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

internal class PluginCreator private constructor(
  val pluginFileName: String,
  val descriptorPath: String,
  private val parentPlugin: PluginCreator?,
  private val problemResolver: PluginCreationResultResolver = IntelliJPluginCreationResultResolver()
) {

  companion object {
    private val LOG = LoggerFactory.getLogger(PluginCreator::class.java)

    private const val MAX_PRODUCT_CODE_LENGTH = 15
    private const val MAX_VERSION_LENGTH = 64
    private const val MAX_PROPERTY_LENGTH = 255
    private const val MAX_LONG_PROPERTY_LENGTH = 65535

    private const val INTELLIJ_THEME_EXTENSION = "com.intellij.themeProvider"

    private val DEFAULT_TEMPLATE_NAMES = setOf("Plugin display name here", "My Framework Support", "Template", "Demo")
    private val PLUGIN_NAME_RESTRICTED_WORDS = setOf(
      "plugin", "JetBrains", "IDEA", "PyCharm", "CLion", "AppCode", "DataGrip", "Fleet", "GoLand", "PhpStorm",
      "WebStorm", "Rider", "ReSharper", "TeamCity", "YouTrack", "RubyMine", "IntelliJ"
    )
    private val DEFAULT_TEMPLATE_DESCRIPTIONS = setOf(
      "Enter short description for your plugin here", "most HTML tags may be used", "example.com/my-framework"
    )

    val v2ModulePrefix = Regex("^intellij\\..*")

    // \u2013 - `–` (short dash) ans \u2014 - `—` (long dash)
    @Suppress("RegExpSimplifiable")
    private val latinSymbolsRegex = Regex("[\\w\\s\\p{Punct}\\u2013\\u2014]{$MIN_DESCRIPTION_LENGTH,}")

    private val json = jacksonObjectMapper()

    private val releaseDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    private val pluginIdVerifier = PluginIdVerifier()
    private val pluginUntilBuildVerifier = PluginUntilBuildVerifier()
    private val pluginProductReleaseVersionVerifier = ProductReleaseVersionVerifier()

    @JvmStatic
    fun createPlugin(
      pluginFile: Path,
      descriptorPath: String,
      parentPlugin: PluginCreator?,
      validateDescriptor: Boolean,
      document: Document,
      documentPath: Path,
      pathResolver: ResourceResolver
    ) = createPlugin(
      pluginFile.simpleName, descriptorPath, parentPlugin, validateDescriptor, document, documentPath, pathResolver
    )

    @JvmStatic
    fun createPlugin(
      pluginFileName: String,
      descriptorPath: String,
      parentPlugin: PluginCreator?,
      validateDescriptor: Boolean,
      document: Document,
      documentPath: Path,
      pathResolver: ResourceResolver
    ): PluginCreator = createPlugin(pluginFileName, descriptorPath,
                                    parentPlugin, validateDescriptor,
                                    document, documentPath,
                                    pathResolver,
                                    IntelliJPluginCreationResultResolver())

    @JvmStatic
    fun createPlugin(
      pluginFileName: String,
      descriptorPath: String,
      parentPlugin: PluginCreator?,
      validateDescriptor: Boolean,
      document: Document,
      documentPath: Path,
      pathResolver: ResourceResolver,
      problemResolver: PluginCreationResultResolver
    ): PluginCreator {
      val pluginCreator = PluginCreator(pluginFileName, descriptorPath, parentPlugin, problemResolver)
      pluginCreator.resolveDocumentAndValidateBean(
        document, documentPath, descriptorPath, pathResolver, validateDescriptor
      )
      return pluginCreator
    }

    @JvmStatic
    fun createInvalidPlugin(pluginFile: Path, descriptorPath: String, singleProblem: PluginProblem) =
      createInvalidPlugin(pluginFile.simpleName, descriptorPath, singleProblem)

    @JvmStatic
    fun createInvalidPlugin(pluginFileName: String, descriptorPath: String, singleProblem: PluginProblem): PluginCreator {
      require(singleProblem.level == ERROR) { "Only ERROR problems are allowed here" }
      val pluginCreator = PluginCreator(pluginFileName, descriptorPath, null)
      pluginCreator.registerProblem(singleProblem)
      return pluginCreator
    }
  }

  val optionalDependenciesConfigFiles: MutableMap<PluginDependency, String> = linkedMapOf()
  val contentModules = arrayListOf<Module>()

  internal val plugin = IdePluginImpl()

  private val problems: MutableList<PluginProblem>
    get() = plugin.problems

  val pluginId: String?
    get() = plugin.pluginId ?: parentPlugin?.pluginId

  val isSuccess: Boolean
    get() = !hasErrors()

  val pluginCreationResult: PluginCreationResult<IdePlugin>
    get() = problemResolver.resolve(plugin, problems)
        .reassignStructureProblems()
        .add(telemetry)

  val telemetry: MutablePluginTelemetry = MutablePluginTelemetry()

  internal val resolvedProblems: List<PluginProblem>
    get() = problemResolver.classify(plugin, problems)

  fun addModuleDescriptor(moduleName: String, configurationFile: String, moduleCreator: PluginCreator) {
    val pluginCreationResult = moduleCreator.pluginCreationResult
    if (pluginCreationResult is PluginCreationSuccess<IdePlugin>) {
      // Content module should be in v2 model
      if (pluginCreationResult.plugin.isV2) {
        val module = pluginCreationResult.plugin
        // TODO: should we distinguish if it's module or plugin dependency
        // Module dependencies are required for module but optional for plugin
        module.dependencies.forEach { pluginDependency ->
          // no need to add already existing dependency
          if (!plugin.dependencies.map { it.id }.contains(pluginDependency.id)) {
            val moduleDependency = PluginDependencyImpl(pluginDependency.id, true, pluginDependency.isModule)
            plugin.dependencies += moduleDependency
          }
        }
        plugin.modulesDescriptors.add(ModuleDescriptor(moduleName, module.dependencies, module, configurationFile))
        plugin.definedModules.add(moduleName)

        mergeContent(module)
      }
    } else {
      registerProblem(ModuleDescriptorResolutionProblem(moduleName, configurationFile, pluginCreationResult.errors))
    }
  }

  internal fun mergeContent(pluginToMerge: IdePlugin) {
    pluginToMerge.extensions.forEach { (extensionPointName, extensionElement) ->
      plugin.extensions.getOrPut(extensionPointName) { arrayListOf() }.addAll(extensionElement)
    }
    if (pluginToMerge is IdePluginImpl) {
      plugin.appContainerDescriptor.mergeWith(pluginToMerge.appContainerDescriptor)
      plugin.projectContainerDescriptor.mergeWith(pluginToMerge.projectContainerDescriptor)
      plugin.moduleContainerDescriptor.mergeWith(pluginToMerge.moduleContainerDescriptor)
    }
  }

  private fun MutableIdePluginContentDescriptor.mergeWith(other: MutableIdePluginContentDescriptor) {
    services += other.services
    components += other.components
    listeners += other.listeners
    extensionPoints += other.extensionPoints
  }

  fun registerOptionalDependenciesConfigurationFilesCycleProblem(configurationFileCycle: List<String>) {
    registerProblem(OptionalDependencyDescriptorCycleProblem(descriptorPath, configurationFileCycle))
  }

  fun setIcons(icons: List<PluginIcon>) {
    plugin.icons = icons
  }

  fun setThirdPartyDependencies(thirdPartyDependencies: List<ThirdPartyDependency>){
    plugin.thirdPartyDependencies = thirdPartyDependencies
  }

  fun setPluginVersion(pluginVersion: String) {
    plugin.pluginVersion = pluginVersion
  }

  fun setOriginalFile(originalFile: Path) {
    plugin.originalFile = originalFile
  }

  fun setHasDotNetPart(hasDotNetPart: Boolean) {
    plugin.hasDotNetPart = hasDotNetPart
  }

  /**
   * Create an instance of an invalid plugin with the most basic information.
   *
   * This allows creating a bare-bones plugin with invalid data.
   * Plugin problems associated with this invalid data might be reclassified by the [problem resolver](#problemResolver).
   */
  private fun newInvalidPlugin(bean: PluginBean, document: Document): InvalidPlugin {
    return InvalidPlugin(document).apply {
      pluginId = bean.id?.trim()
      pluginName = bean.name?.trim()
      bean.vendor?.let {
        vendor = if (it.name != null) it.name.trim() else null
        vendorUrl = it.url
        vendorEmail = it.email
      }
    }
  }

  private fun IdePluginImpl.setInfoFromBean(bean: PluginBean, document: Document) {
    pluginName = bean.name?.trim()
    pluginId = bean.id?.trim() ?: pluginName
    url = bean.url?.trim()
    pluginVersion = if (bean.pluginVersion != null) bean.pluginVersion.trim() else null
    definedModules.addAll(bean.modules)
    useIdeClassLoader = bean.useIdeaClassLoader == true
    isImplementationDetail = bean.implementationDetail == true

    val ideaVersionBean = bean.ideaVersion
    if (ideaVersionBean != null) {
      sinceBuild = if (ideaVersionBean.sinceBuild != null) IdeVersion.createIdeVersion(ideaVersionBean.sinceBuild) else null
      var untilBuild: String? = ideaVersionBean.untilBuild
      if (untilBuild != null && untilBuild.isNotEmpty()) {
        if (untilBuild.endsWith(".*")) {
          val idx = untilBuild.lastIndexOf('.')
          untilBuild = untilBuild.substring(0, idx + 1) + Integer.MAX_VALUE
        }
        this.untilBuild = IdeVersion.createIdeVersion(untilBuild)
      }
    }

    isV2 = bean.packageName != null

    val modulePrefix = "com.intellij.modules."

    if (bean.dependencies != null) {
      for (dependencyBean in bean.dependencies) {
        if (dependencyBean.dependencyId != null) {
          val isModule = dependencyBean.dependencyId.startsWith(modulePrefix)
          val isOptional = java.lang.Boolean.TRUE == dependencyBean.optional
          val dependency = PluginDependencyImpl(dependencyBean.dependencyId, isOptional, isModule)
          dependencies += dependency

          if (dependency.isOptional && dependencyBean.configFile != null) {
            //V2 dependency configs can be located only in root
            optionalDependenciesConfigFiles[dependency] =
              if (v2ModulePrefix.matches(dependencyBean.configFile)) "../${dependencyBean.configFile}" else dependencyBean.configFile
          }
        }
      }
    }

    if (bean.dependenciesV2 != null) {
      for (dependencyBeanV2 in bean.dependenciesV2.modules) {
        if (dependencyBeanV2.moduleName != null) {
          val dependency = PluginDependencyImpl(dependencyBeanV2.moduleName, false, true)
          dependencies += dependency
        }
      }
      for (dependencyBeanV2 in bean.dependenciesV2.plugins) {
        if (dependencyBeanV2.dependencyId != null) {
          val isModule = dependencyBeanV2.dependencyId.startsWith(modulePrefix)
          val dependency = PluginDependencyImpl(dependencyBeanV2.dependencyId, false, isModule)
          dependencies += dependency
        }
      }
    }

    if (bean.pluginContent != null) {
      val modules = bean.pluginContent.flatMap { it.modules }

      for (pluginModule in modules) {
        val name = pluginModule.moduleName
        if (name != null) {
          val configFile = "../${name.replace("/", ".")}.xml"
          contentModules += Module(name, configFile)
        }
      }
    }

    bean.incompatibleModules?.filter { it?.startsWith(modulePrefix) ?: false }?.let {
      incompatibleModules += it
    }

    val vendorBean = bean.vendor
    if (vendorBean != null) {
      vendor = if (vendorBean.name != null) vendorBean.name.trim { it <= ' ' } else null
      vendorUrl = vendorBean.url
      vendorEmail = vendorBean.email
    }
    val productDescriptorBean = bean.productDescriptor
    if (productDescriptorBean != null) {
      productDescriptor = ProductDescriptor(
        productDescriptorBean.code,
        LocalDate.parse(productDescriptorBean.releaseDate, releaseDateFormatter),
        ProductReleaseVersion.parse(productDescriptorBean.releaseVersion),
        productDescriptorBean.eap == "true",
        productDescriptorBean.optional == "true"
      )
    }
    changeNotes = bean.changeNotes
    description = bean.description

    val rootElement = document.rootElement
    readActions(rootElement, this)

    readExtensions(rootElement, this)
    readExtensionPoints(rootElement, this)

    readListeners(rootElement, "applicationListeners", appContainerDescriptor)
    readListeners(rootElement, "projectListeners", projectContainerDescriptor)

    readComponents(rootElement, "application-components", appContainerDescriptor)
    readComponents(rootElement, "project-components", projectContainerDescriptor)
    readComponents(rootElement, "module-components", moduleContainerDescriptor)
  }

  private fun readActions(rootElement: Element, idePlugin: IdePluginImpl) {
    for (actionsRoot in rootElement.getChildren("actions")) {
      idePlugin.actions += actionsRoot.children
    }
  }

  private fun readExtensions(rootElement: Element, idePlugin: IdePluginImpl) {
    for (extensionsRoot in rootElement.getChildren("extensions")) {
      for (extensionElement in extensionsRoot.children) {
        when (val epName = extractEPName(extensionElement)) {
          "com.intellij.applicationService" -> idePlugin.appContainerDescriptor.services += readServiceDescriptor(extensionElement, epName)
          "com.intellij.projectService" -> idePlugin.projectContainerDescriptor.services += readServiceDescriptor(extensionElement, epName)
          "com.intellij.moduleService" -> idePlugin.moduleContainerDescriptor.services += readServiceDescriptor(extensionElement, epName)
          "org.jetbrains.kotlin.supportsKotlinPluginMode" -> {
            idePlugin.addExtension(epName, extensionElement)
            idePlugin.kotlinPluginMode = readKotlinPluginMode(extensionElement)
          }
          else -> idePlugin.addExtension(epName, extensionElement)
        }
      }
    }
  }

  private fun IdePluginImpl.addExtension(epName: String, extensionElement: Element) {
    extensions.getOrPut(epName) { arrayListOf() }.add(extensionElement)
  }

  private fun readExtensionPoints(rootElement: Element, idePlugin: IdePluginImpl) {
    for (extensionPointsRoot in rootElement.getChildren("extensionPoints")) {
      for (extensionPoint in extensionPointsRoot.children) {
        val extensionPointName = getExtensionPointName(extensionPoint) ?: continue
        val containerDescriptor = when (extensionPoint.getAttributeValue("area")) {
          null -> idePlugin.appContainerDescriptor
          "IDEA_APPLICATION" -> idePlugin.appContainerDescriptor
          "IDEA_PROJECT" -> idePlugin.projectContainerDescriptor
          "IDEA_MODULE" -> idePlugin.moduleContainerDescriptor
          else -> null
        } ?: continue
        val isDynamic = extensionPoint.getAttributeValue("dynamic")?.toBoolean() ?: false
        containerDescriptor.extensionPoints += IdePluginContentDescriptor.ExtensionPoint(extensionPointName, isDynamic)
      }
    }
  }

  private fun getExtensionPointName(extensionPoint: Element): String? {
    extensionPoint.getAttributeValue("qualifiedName")?.let { return it }
    val name = extensionPoint.getAttributeValue("name") ?: return null
    val pluginId = pluginId ?: return null
    return "$pluginId.$name"
  }

  fun Element.getAttributeBooleanValue(attname: String, default: Boolean): Boolean {
    return getAttributeValue(attname)?.toBoolean() ?: default
  }

  private fun Element.readServicePreloadMode() =
    when (getAttributeValue("preload")) {
      "true" -> IdePluginContentDescriptor.PreloadMode.TRUE
      "await" -> IdePluginContentDescriptor.PreloadMode.AWAIT
      "notHeadless" -> IdePluginContentDescriptor.PreloadMode.NOT_HEADLESS
      "notLightEdit" -> IdePluginContentDescriptor.PreloadMode.NOT_LIGHT_EDIT
      null -> IdePluginContentDescriptor.PreloadMode.FALSE
      else -> IdePluginContentDescriptor.PreloadMode.FALSE.also { LOG.error("Unknown preload mode value '${getAttributeValue("preload")}'") }
    }

  private fun Element.readServiceClient() =
    when (getAttributeValue("client")) {
      "all" -> IdePluginContentDescriptor.ClientKind.ALL
      "local" -> IdePluginContentDescriptor.ClientKind.LOCAL
      "guest" -> IdePluginContentDescriptor.ClientKind.GUEST
      "controller" -> IdePluginContentDescriptor.ClientKind.CONTROLLER
      "owner" -> IdePluginContentDescriptor.ClientKind.OWNER
      "remote" -> IdePluginContentDescriptor.ClientKind.REMOTE
      "frontend" -> IdePluginContentDescriptor.ClientKind.FRONTEND
      null -> null
      else -> null.also {
        registerProblem(UnknownServiceClientValue(descriptorPath, getAttributeValue("client")))
      }
    }

  private fun readServiceDescriptor(extensionElement: Element, epName: String): IdePluginContentDescriptor.ServiceDescriptor {
    val serviceInterface = extensionElement.getAttributeValue("serviceInterface")
    val serviceImplementation = extensionElement.getAttributeValue("serviceImplementation")
    val serviceType = IdePluginContentDescriptor.ServiceType.valueOf(epName.replace("(Service)|(com.intellij.)".toRegex(), "").toUpperCase())
    val testServiceImplementation = extensionElement.getAttributeValue("testServiceImplementation")
    val headlessImplementation = extensionElement.getAttributeValue("headlessImplementation")
    val configurationSchemaKey = extensionElement.getAttributeValue("configurationSchemaKey")
    val overrides = extensionElement.getAttributeBooleanValue("overrides", false)
    val preload = extensionElement.readServicePreloadMode()
    val client = extensionElement.readServiceClient()
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

  private fun Element.readOs() = when (getAttributeValue("os")) {
    "mac" -> IdePluginContentDescriptor.Os.mac
    "linux" -> IdePluginContentDescriptor.Os.linux
    "windows" -> IdePluginContentDescriptor.Os.windows
    "unix" -> IdePluginContentDescriptor.Os.unix
    "freebsd" -> IdePluginContentDescriptor.Os.freebsd
    null -> null
    else -> null.also { LOG.error("Unknown OS: ${getAttributeValue("os")}") }
  }

  private fun readListeners(rootElement: Element, listenersName: String, containerDescriptor: MutableIdePluginContentDescriptor) {
    for (listenersRoot in rootElement.getChildren(listenersName)) {
      for (listener in listenersRoot.children) {
        val className = listener.getAttributeValue("class")
        val topicName = listener.getAttributeValue("topic")
        val isActiveInTestMode = listener.getAttributeBooleanValue("activeInTestMode", true)
        val isActiveInHeadlessMode = listener.getAttributeBooleanValue("activeInHeadlessMode", true)
        val os: IdePluginContentDescriptor.Os? = listener.readOs()
        if (className == null) {
          registerProblem(ElementMissingAttribute("listener", "class"))
        }
        if (topicName == null) {
          registerProblem(ElementMissingAttribute("listener", "topic"))
        }
        if (className != null && topicName != null) {
          val listenerType = IdePluginContentDescriptor.ListenerType.valueOf(listenersName.replace("Listeners", "").toUpperCase())
          containerDescriptor.listeners += IdePluginContentDescriptor.ListenerDescriptor(topicName, className, listenerType, isActiveInTestMode, isActiveInHeadlessMode, os)
        }
      }
    }
  }

  private fun readComponents(rootElement: Element, componentsArea: String, containerDescriptor: MutableIdePluginContentDescriptor) {
    for (componentsRoot in rootElement.getChildren(componentsArea)) {
      for (component in componentsRoot.getChildren("component")) {
        val interfaceClass = component.getChild("interface-class")?.text
        val implementationClass = component.getChild("implementation-class")?.text
        if (implementationClass != null) {
          containerDescriptor.components += IdePluginContentDescriptor.ComponentConfig(interfaceClass, implementationClass)
        }
      }
    }
  }

  private fun readKotlinPluginMode(extensionElement: Element): KotlinPluginMode {
    val supportsK1 = extensionElement.getAttributeBooleanValue("supportsK1", true)
    val supportsK2 = extensionElement.getAttributeBooleanValue("supportsK2", false)
    return KotlinPluginMode.parse(supportsK1, supportsK2)
  }

  private fun validatePluginBean(bean: PluginBean, validateDescriptor: Boolean) {
    if (validateDescriptor || bean.url != null) {
      validateBeanUrl(bean.url)
    }
    if (validateDescriptor || bean.id != null) {
      validateId(bean)
    }
    if (validateDescriptor || bean.name != null) {
      validateName(bean.name)
    }
    if (validateDescriptor || bean.pluginVersion != null) {
      validateVersion(bean.pluginVersion, validateDescriptor)
    }
    if (validateDescriptor || bean.description != null) {
      validateDescription(bean.description, validateDescriptor)
    }
    if (validateDescriptor || bean.changeNotes != null) {
      validateChangeNotes(bean.changeNotes)
    }
    if (validateDescriptor || bean.vendor != null) {
      validateVendor(bean.vendor)
    }
    if (validateDescriptor || bean.ideaVersion != null) {
      validateIdeaVersion(bean.ideaVersion)
      pluginUntilBuildVerifier.verify(bean, descriptorPath, ::registerProblem)
    }
    if (validateDescriptor || bean.productDescriptor != null) {
      validateProductDescriptor(bean, bean.productDescriptor)
    }
    if (bean.dependencies != null) {
      validateDependencies(bean.dependencies)
    }

    if (bean.modules?.any { it.isEmpty() } == true) {
      registerProblem(InvalidModuleBean(descriptorPath))
    }
  }

  private fun validateDependencies(dependencies: List<PluginDependencyBean>) {
    for (dependencyBean in dependencies) {
      if (dependencyBean.dependencyId.isNullOrBlank() || dependencyBean.dependencyId.contains("\n")) {
        registerProblem(InvalidDependencyId(descriptorPath, dependencyBean.dependencyId))
      } else if (dependencyBean.optional == true) {
        if (dependencyBean.configFile == null) {
          registerProblem(OptionalDependencyConfigFileNotSpecified(dependencyBean.dependencyId))
        } else if (dependencyBean.configFile.isBlank()) {
          registerProblem(OptionalDependencyConfigFileIsEmpty(dependencyBean.dependencyId, descriptorPath))
        }
      } else if (dependencyBean.optional == false) {
        registerProblem(SuperfluousNonOptionalDependencyDeclaration(dependencyBean.dependencyId))
      }
    }
    ReusedDescriptorVerifier(descriptorPath).verify(dependencies, ::registerProblem)
  }

  private fun validateProductDescriptor(plugin: PluginBean, productDescriptor: ProductDescriptorBean?) {
    if (productDescriptor != null) {
      validateProductCode(productDescriptor.code)
      validateReleaseDate(productDescriptor.releaseDate)
      pluginProductReleaseVersionVerifier.verify(plugin, descriptorPath, ::registerProblem)
      productDescriptor.eap?.let { validateEapFlag(it) }
      productDescriptor.optional?.let { validateOptionalFlag(it) }
    }
  }

  private fun validateProductCode(productCode: String?) {
    if (productCode.isNullOrEmpty()) {
      registerProblem(PropertyNotSpecified("code", descriptorPath))
    } else {
      validatePropertyLength("Product code", productCode, MAX_PRODUCT_CODE_LENGTH)
    }
  }

  private fun validateReleaseDate(releaseDate: String?) {
    if (releaseDate.isNullOrEmpty()) {
      registerProblem(PropertyNotSpecified("release-date", descriptorPath))
    } else {
      try {
        val date = LocalDate.parse(releaseDate, releaseDateFormatter)
        if (date > LocalDate.now().plusDays(5)) {
          registerProblem(ReleaseDateInFuture(descriptorPath))
        }
      } catch (e: DateTimeParseException) {
        registerProblem(ReleaseDateWrongFormat(descriptorPath))
      }
    }
  }

  private fun validateEapFlag(eapFlag: String) = validateBooleanFlag(eapFlag, "eap")

  private fun validateOptionalFlag(optionalFlag: String) = validateBooleanFlag(optionalFlag, "optional")

  private fun validateBooleanFlag(flag: String, name: String) {
    if (flag != "true" && flag != "false") {
      registerProblem(NotBoolean(name, descriptorPath))
    }
  }

  private fun validateBeanUrl(beanUrl: String?) {
    if (beanUrl != null) {
      validatePropertyLength("plugin url", beanUrl, MAX_PROPERTY_LENGTH)
    }
  }

  private fun validateVersion(pluginVersion: String?, validateDescriptor: Boolean) {
    if (!validateDescriptor && pluginVersion == null) {
      return
    }
    if (pluginVersion.isNullOrEmpty()) {
      registerProblem(PropertyNotSpecified("version", descriptorPath))
    } else {
      validatePropertyLength("version", pluginVersion, MAX_VERSION_LENGTH)
    }
  }

  private fun validatePlugin(plugin: IdePluginImpl) {
    val dependencies = plugin.dependencies
    if (!plugin.isV2) {
      if (dependencies.isEmpty()) {
        registerProblem(NoDependencies(descriptorPath))
      }
      if (dependencies.count { it.isModule } == 0) {
        registerProblem(NoModuleDependencies(descriptorPath))
      }
    }
    dependencies.map { it.id }
      .groupingBy { it }
      .eachCount()
      .filterValues { it > 1 }
      .map { it.key }
      .forEach { duplicatedDependencyId -> registerProblem(DuplicatedDependencyWarning(duplicatedDependencyId)) }

    val sinceBuild = plugin.sinceBuild
    val untilBuild = plugin.untilBuild
    if (sinceBuild != null && untilBuild != null && sinceBuild > untilBuild) {
      registerProblem(SinceBuildGreaterThanUntilBuild(descriptorPath, sinceBuild, untilBuild))
    }

    val listenersAvailableSinceBuild = IdeVersion.createIdeVersion("193")
    if (sinceBuild != null && sinceBuild < listenersAvailableSinceBuild) {
      if (plugin.appContainerDescriptor.listeners.isNotEmpty()) {
        registerProblem(ElementAvailableOnlySinceNewerVersion("applicationListeners", listenersAvailableSinceBuild, sinceBuild, untilBuild))
      }
      if (plugin.projectContainerDescriptor.listeners.isNotEmpty()) {
        registerProblem(ElementAvailableOnlySinceNewerVersion("projectListeners", listenersAvailableSinceBuild, sinceBuild, untilBuild))
      }
    }

    ServiceExtensionPointPreloadVerifier().verify(plugin, ::registerProblem)
    StatusBarWidgetFactoryExtensionPointVerifier().verify(plugin, ::registerProblem)
    K2IdeModeCompatibilityVerifier().verify(plugin, ::registerProblem, descriptorPath)
  }

  private fun resolveDocumentAndValidateBean(
    originalDocument: Document,
    documentPath: Path,
    documentName: String,
    pathResolver: ResourceResolver,
    validateDescriptor: Boolean
  ) {
    val document = resolveXIncludesOfDocument(originalDocument, documentName, pathResolver, documentPath) ?: return
    val bean = readDocumentIntoXmlBean(document) ?: return
    validatePluginBean(bean, validateDescriptor)
    if (hasPluginBeanErrors(bean, document)) {
      return
    }

    plugin.underlyingDocument = document
    plugin.setInfoFromBean(bean, document)

    val themeFiles = readPluginThemes(plugin, documentPath, pathResolver) ?: return
    plugin.declaredThemes.addAll(themeFiles)

    validatePlugin(plugin)
  }

  private fun readPluginThemes(plugin: IdePlugin, document: Path, pathResolver: ResourceResolver): List<IdeTheme>? {
    val themePaths = plugin.extensions[INTELLIJ_THEME_EXTENSION]?.mapNotNull {
      it.getAttribute("path")?.value
    } ?: emptyList()

    val themes = arrayListOf<IdeTheme>()

    for (themePath in themePaths) {
      val absolutePath = if (themePath.startsWith("/")) themePath else "/$themePath"
      when (val resolvedTheme = pathResolver.resolveResource(absolutePath, document)) {
        is ResourceResolver.Result.Found -> resolvedTheme.use {
          val theme = try {
            val themeJson = it.resourceStream.reader().readText()
            json.readValue(themeJson, IdeTheme::class.java)
          } catch (e: Exception) {
            registerProblem(UnableToReadTheme(descriptorPath, themePath))
            return null
          }
          themes.add(theme)
        }
        is ResourceResolver.Result.NotFound -> {
          registerProblem(UnableToFindTheme(descriptorPath, themePath))
        }
        is ResourceResolver.Result.Failed -> {
          registerProblem(UnableToReadTheme(descriptorPath, themePath))
        }
      }
    }
    return themes
  }

  private fun resolveXIncludesOfDocument(
    document: Document,
    presentablePath: String,
    pathResolver: ResourceResolver,
    documentPath: Path
  ): Document? = try {
    XIncluder.resolveXIncludes(document, presentablePath, pathResolver, documentPath)
  } catch (e: XIncluderException) {
    LOG.info("Unable to resolve <xi:include> elements of descriptor '$descriptorPath' from '$pluginFileName'", e)
    registerProblem(XIncludeResolutionErrors(descriptorPath, e.message))
    null
  }

  private fun readDocumentIntoXmlBean(document: Document): PluginBean? {
    return try {
      PluginBeanExtractor.extractPluginBean(document)
    } catch (e: Exception) {
      registerProblem(UnableToReadDescriptor(descriptorPath, e.localizedMessage))
      LOG.info("Unable to read plugin descriptor $descriptorPath of $pluginFileName", e)
      null
    }

  }

  internal fun registerProblem(problem: PluginProblem) {
    problems += problem
  }

  private fun hasErrors(): Boolean {
    return problemResolver.classify(plugin, problems).any {
      it.level == ERROR
    }
  }

  /**
   * Checks if the _bean_ has any core plugin bean errors in the plugin descriptor.
   *
   * Such core errors are field validation errors (missing fields, empty values).
   * If any such core error is found, an instance of invalid plugin with the most
   * basic information is created and filtered by [problem resolver](problemResolver).
   *
   * This enables problem resolver to intentionally ignore core plugin bean errors
   * and reclassify these errors into warnings or even ignore them.
   *
   * The usual example is an internal JetBrains plugin that might violate rules for
   * allowed field values in the plugin descriptor.
   *
   * @see validatePluginBean
   */
  private fun hasPluginBeanErrors(bean: PluginBean, document: Document): Boolean {
    if (problems.isEmpty()) {
      return false
    }
    val invalidPlugin = newInvalidPlugin(bean, document)
    return problemResolver
      .classify(invalidPlugin, problems)
      .filter {
        it.level == ERROR
      }.notEmpty(context = bean)
  }

  private fun validateId(plugin: PluginBean) {
    pluginIdVerifier.verify(plugin, descriptorPath, ::registerProblem)
  }

  private fun validateName(name: String?) {
    when {
      name.isNullOrBlank() -> registerProblem(PropertyNotSpecified("name", descriptorPath))
      DEFAULT_TEMPLATE_NAMES.any { it.equals(name, true) } -> {
        registerProblem(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.NAME, name))
      }
      else -> {
        val templateWord = PLUGIN_NAME_RESTRICTED_WORDS.find { name.contains(it, true) }
        if (templateWord != null) {
          registerProblem(TemplateWordInPluginName(descriptorPath, name, templateWord))
        }
        validatePropertyLength("name", name, MAX_NAME_LENGTH)
        verifyNewlines("name", name, descriptorPath, ::registerProblem)
      }
    }
  }

  private fun validateDescription(htmlDescription: String?, validateDescriptor: Boolean) {
    if (!validateDescriptor && htmlDescription == null) {
      return
    }
    if (htmlDescription.isNullOrEmpty()) {
      registerProblem(PropertyNotSpecified("description", descriptorPath))
      return
    }
    validatePropertyLength("description", htmlDescription, MAX_LONG_PROPERTY_LENGTH)

    val html = Jsoup.parseBodyFragment(htmlDescription)
    val textDescription = html.text()

    if (DEFAULT_TEMPLATE_DESCRIPTIONS.any { textDescription.contains(it) }) {
      registerProblem(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.DESCRIPTION, textDescription))
      return
    }

    val latinDescriptionPart = latinSymbolsRegex.find(textDescription)?.value
    if (latinDescriptionPart == null) {
      registerProblem(ShortOrNonLatinDescription())
    }
    val links = html.select("[href],img[src]")
    links.forEach { link ->
      val href = link.attr("abs:href")
      val src = link.attr("abs:src")
      if (href.startsWith("http://")) {
        registerProblem(HttpLinkInDescription(href))
      }
      if (src.startsWith("http://")) {
        registerProblem(HttpLinkInDescription(src))
      }
    }
  }

  private fun validateChangeNotes(changeNotes: String?) {
    if (changeNotes.isNullOrBlank()) {
      //Too many plugins don't specify the change-notes, so it's too strict to require them.
      //But if specified, let's check that the change-notes are long enough.
      return
    }

    if (changeNotes.contains("Add change notes here") || changeNotes.contains("most HTML tags may be used")) {
      registerProblem(DefaultChangeNotes(descriptorPath))
    }

    validatePropertyLength("<change-notes>", changeNotes, MAX_LONG_PROPERTY_LENGTH)
  }

  private fun validatePropertyLength(propertyName: String, propertyValue: String, maxLength: Int) {
    if (propertyValue.length > maxLength) {
      registerProblem(TooLongPropertyValue(descriptorPath, propertyName, propertyValue.length, maxLength))
    }
  }

  private fun validateVendor(vendorBean: PluginVendorBean?) {
    if (vendorBean == null) {
      registerProblem(PropertyNotSpecified("vendor", descriptorPath))
      return
    }

    if (vendorBean.name.isNullOrBlank()) {
      registerProblem(VendorCannotBeEmpty(descriptorPath))
      return
    }

    if ("YourCompany" == vendorBean.name) {
      registerProblem(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.VENDOR, vendorBean.name))
    }
    validatePropertyLength("vendor", vendorBean.name, MAX_PROPERTY_LENGTH)

    if ("https://www.yourcompany.com" == vendorBean.url) {
      registerProblem(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.VENDOR_URL, vendorBean.url))
    }
    validatePropertyLength("vendor url", vendorBean.url, MAX_PROPERTY_LENGTH)

    if ("support@yourcompany.com" == vendorBean.email) {
      registerProblem(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.VENDOR_EMAIL, vendorBean.email))
    }
    validatePropertyLength("vendor email", vendorBean.email, MAX_PROPERTY_LENGTH)
  }

  private fun validateSinceBuild(sinceBuild: String?) {
    if (sinceBuild == null) {
      registerProblem(SinceBuildNotSpecified(descriptorPath))
    } else {
      val sinceBuildParsed = IdeVersion.createIdeVersionIfValid(sinceBuild)
      if (sinceBuildParsed == null) {
        registerProblem(InvalidSinceBuild(descriptorPath, sinceBuild))
      } else {
        if (sinceBuild.endsWith(".*")) {
          registerProblem(SinceBuildCannotContainWildcard(descriptorPath, sinceBuildParsed))
        }
        if (sinceBuildParsed.baselineVersion < 130) {
          registerProblem(InvalidSinceBuild(descriptorPath, sinceBuild))
        }
        if (sinceBuildParsed.baselineVersion > 999) {
          registerProblem(ErroneousSinceBuild(descriptorPath, sinceBuildParsed))
        }
        if (sinceBuildParsed.productCode.isNotEmpty()) {
          registerProblem(ProductCodePrefixInBuild(descriptorPath))
        }
      }
    }
  }

  private fun validateIdeaVersion(versionBean: IdeaVersionBean?) {
    if (versionBean == null) {
      registerProblem(PropertyNotSpecified("idea-version", descriptorPath))
      return
    }

    val sinceBuild = versionBean.sinceBuild
    validateSinceBuild(sinceBuild)
  }

  private val PluginCreationResult<IdePlugin>.errors: List<PluginProblem>
    get() = when (this) {
      is PluginCreationSuccess -> emptyList()
      is PluginCreationFail -> this.errorsAndWarnings.filter { it.level === ERROR }
    }

  private fun PluginCreationResult<IdePlugin>.reassignStructureProblems() =
    when (this) {
      is PluginCreationSuccess -> copy(plugin = IdePluginImpl.clone(plugin, problems))
      is PluginCreationFail -> this
    }

  private val PluginCreationSuccess<IdePlugin>.problems: List<PluginProblem>
    get() = warnings + unacceptableWarnings

  private fun List<PluginProblem>.notEmpty(context: PluginBean): Boolean {
    return if (isEmpty()) {
      false
    } else {
      if (LOG.isDebugEnabled) {
        val errorMsg = joinToString()
        LOG.debug("Plugin '${context.id}' has $size error(s): $errorMsg")
      }
      true
    }
  }

}

private fun PluginCreationResult<IdePlugin>.add(telemetry: PluginTelemetry): PluginCreationResult<IdePlugin> {
  return when (this) {
    is PluginCreationSuccess -> this.copy(telemetry = telemetry)
    is PluginCreationFail -> this
  }
}
