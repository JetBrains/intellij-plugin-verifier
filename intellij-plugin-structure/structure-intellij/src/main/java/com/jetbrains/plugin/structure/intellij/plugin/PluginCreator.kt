/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.base.plugin.ThirdPartyDependency
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PluginProblem.Level.ERROR
import com.jetbrains.plugin.structure.base.telemetry.MutablePluginTelemetry
import com.jetbrains.plugin.structure.base.telemetry.PluginTelemetry
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.intellij.beans.PluginBean
import com.jetbrains.plugin.structure.intellij.beans.PluginDependencyBean
import com.jetbrains.plugin.structure.intellij.plugin.PluginDescriptorParser.ParseResult.Parsed
import com.jetbrains.plugin.structure.intellij.plugin.ValidationContext.ValidationResult
import com.jetbrains.plugin.structure.intellij.plugin.descriptors.DescriptorResource
import com.jetbrains.plugin.structure.intellij.plugin.loaders.PluginThemeLoader
import com.jetbrains.plugin.structure.intellij.problems.DuplicatedDependencyWarning
import com.jetbrains.plugin.structure.intellij.problems.ElementMissingAttribute
import com.jetbrains.plugin.structure.intellij.problems.IntelliJPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.NoDependencies
import com.jetbrains.plugin.structure.intellij.problems.NoModuleDependencies
import com.jetbrains.plugin.structure.intellij.problems.OptionalDependencyDescriptorCycleProblem
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.SinceBuildGreaterThanUntilBuild
import com.jetbrains.plugin.structure.intellij.problems.UnknownServiceClientValue
import com.jetbrains.plugin.structure.intellij.resources.PluginArchiveResource
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.verifiers.ExposedModulesVerifier
import com.jetbrains.plugin.structure.intellij.verifiers.K2IdeModeCompatibilityVerifier
import com.jetbrains.plugin.structure.intellij.verifiers.ProjectAndApplicationListenerAvailabilityVerifier
import com.jetbrains.plugin.structure.intellij.verifiers.ServiceExtensionPointPreloadVerifier
import com.jetbrains.plugin.structure.intellij.verifiers.StatusBarWidgetFactoryExtensionPointVerifier
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.intellij.version.ProductReleaseVersion
import org.jdom2.Document
import org.jdom2.Element
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal class PluginCreator private constructor(
  val pluginFileName: String,
  val descriptorPath: String,
  private val parentPlugin: PluginCreator?,
  private val problemResolver: PluginCreationResultResolver = IntelliJPluginCreationResultResolver()
) {

  companion object {
    private val LOG = LoggerFactory.getLogger(PluginCreator::class.java)

    private const val INTELLIJ_THEME_EXTENSION = "com.intellij.themeProvider"

    val v2ModulePrefix = Regex("^intellij\\..*")

    private val json = jacksonObjectMapper()

    private val releaseDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    private val pluginModuleResolver = PluginModuleResolver()

    private val themeLoader = PluginThemeLoader()

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
    ): PluginCreator = createPlugin(
      pluginFileName, descriptorPath,
      parentPlugin, validateDescriptor,
      document, documentPath,
      pathResolver,
      IntelliJPluginCreationResultResolver()
    )

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
    fun createPlugin(
      descriptorResource: DescriptorResource,
      parentPlugin: PluginCreator?,
      document: Document,
      pathResolver: ResourceResolver,
      problemResolver: PluginCreationResultResolver
    ): PluginCreator {
      val pluginCreator =
        PluginCreator(descriptorResource.artifactFileName, descriptorResource.fileName, parentPlugin, problemResolver)
      pluginCreator.resolveDocumentAndValidateBean(
        document, descriptorResource.filePath, descriptorResource.fileName, pathResolver, validateDescriptor = true
      )
      return pluginCreator
    }

    @JvmStatic
    fun createInvalidPlugin(pluginFile: Path, descriptorPath: String, singleProblem: PluginProblem) =
      createInvalidPlugin(pluginFile.simpleName, descriptorPath, singleProblem)

    @JvmStatic
    fun createInvalidPlugin(
      pluginFileName: String,
      descriptorPath: String,
      singleProblem: PluginProblem
    ): PluginCreator {
      require(singleProblem.level == ERROR) { "Only ERROR problems are allowed here" }
      val pluginCreator = PluginCreator(pluginFileName, descriptorPath, null)
      pluginCreator.registerProblem(singleProblem)
      return pluginCreator
    }
  }

  val optionalDependenciesConfigFiles: MutableMap<PluginDependency, String> = linkedMapOf()

  internal val plugin = IdePluginImpl()

  private var invalidPlugin: InvalidPlugin? = null

  private val problems: MutableList<PluginProblem>
    get() = invalidPlugin?.problems ?: plugin.problems

  val pluginId: String?
    get() = plugin.pluginId ?: parentPlugin?.pluginId

  val isSuccess: Boolean
    get() = !hasErrors()

  val pluginCreationResult: PluginCreationResult<IdePlugin>
    get() {
      val invalidPlugin = invalidPlugin
      if (invalidPlugin != null) {
        return PluginCreationFail<IdePlugin>(invalidPlugin.problems)
      }

      return problemResolver.resolve(resolvePlugin(), problems)
        .propagateResources()
        .reassignStructureProblems()
        .add(telemetry)
    }

  internal val resources = mutableListOf<PluginArchiveResource>()

  val telemetry: MutablePluginTelemetry = MutablePluginTelemetry()

  internal val resolvedProblems: List<PluginProblem>
    get() = problemResolver.classify(resolvePlugin(), problems)

  private fun hasErrors(): Boolean {
    val invalidPlugin = invalidPlugin
    if (invalidPlugin != null) {
      return invalidPlugin.problems.isNotEmpty()
    }

    return problemResolver.classify(resolvePlugin(), problems).any {
      it.level == ERROR
    }
  }

  private fun resolvePlugin(): IdePlugin {
    return invalidPlugin ?: plugin
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

  fun setThirdPartyDependencies(thirdPartyDependencies: List<ThirdPartyDependency>) {
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

  fun setClasspath(classpath: Classpath) {
    plugin.classpath = classpath
  }

  fun setPluginIdIfNull(id: String) {
    if (plugin.pluginId == null) {
      plugin.pluginId = id
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
      sinceBuild =
        if (ideaVersionBean.sinceBuild != null) IdeVersion.createIdeVersion(ideaVersionBean.sinceBuild) else null
      var untilBuild: String? = ideaVersionBean.untilBuild
      if (untilBuild != null && untilBuild.isNotEmpty()) {
        if (untilBuild.endsWith(".*")) {
          val idx = untilBuild.lastIndexOf('.')
          untilBuild = untilBuild.substring(0, idx + 1) + Integer.MAX_VALUE
        }
        this.untilBuild = IdeVersion.createIdeVersion(untilBuild)
      }
    }

    hasPackagePrefix = bean.packageName != null

    val modulePrefix = "com.intellij.modules."

    // dependencies from `<depends>`
    dependencies += bean.dependenciesV1.map { depBean ->
      PluginDependencyImpl(depBean.dependencyId, depBean.isOptional, depBean.isModule).also { it ->
        registerIfOptionalDependency(it, depBean)
      }
    }
    // dependencies from `<dependencies>`
    dependencies += bean.dependentModules.map { ModuleV2Dependency(it.moduleName) }
    dependencies += bean.dependentPlugins.map { PluginV2Dependency(it.dependencyId) }

    if (pluginModuleResolver.supports(bean)) {
      contentModules += pluginModuleResolver.resolvePluginModules(bean)
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
          "com.intellij.applicationService" -> idePlugin.appContainerDescriptor.services += readServiceDescriptor(
            extensionElement,
            epName
          )

          "com.intellij.projectService" -> idePlugin.projectContainerDescriptor.services += readServiceDescriptor(
            extensionElement,
            epName
          )

          "com.intellij.moduleService" -> idePlugin.moduleContainerDescriptor.services += readServiceDescriptor(
            extensionElement,
            epName
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
      else -> IdePluginContentDescriptor.PreloadMode.FALSE.also {
        LOG.error(
          "Unknown preload mode value '${
            getAttributeValue(
              "preload"
            )
          }'"
        )
      }
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

  private fun readServiceDescriptor(
    extensionElement: Element,
    epName: String
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

  private fun readListeners(
    rootElement: Element,
    listenersName: String,
    containerDescriptor: MutableIdePluginContentDescriptor
  ) {
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

  private fun readKotlinPluginMode(extensionElement: Element): KotlinPluginMode {
    val supportsK1 = extensionElement.getAttributeBooleanValue("supportsK1", true)
    val supportsK2 = extensionElement.getAttributeBooleanValue("supportsK2", false)
    return KotlinPluginMode.parse(supportsK1, supportsK2)
  }

  private fun validatePlugin(plugin: IdePluginImpl) {
    val dependencies = plugin.dependencies
    if (!plugin.hasPackagePrefix && plugin.contentModules.isEmpty()) {
      if (dependencies.isEmpty()) {
        registerProblem(NoDependencies(descriptorPath))
      }
      if (dependencies.none { it.isModule || it is PluginV2Dependency }) {
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

    ProjectAndApplicationListenerAvailabilityVerifier().verify(plugin, ::registerProblem)
    ServiceExtensionPointPreloadVerifier().verify(plugin, ::registerProblem)
    StatusBarWidgetFactoryExtensionPointVerifier().verify(plugin, ::registerProblem)
    K2IdeModeCompatibilityVerifier().verify(plugin, ::registerProblem, descriptorPath)
    ExposedModulesVerifier().verify(plugin, ::registerProblem, descriptorPath)
  }

  private fun resolveDocumentAndValidateBean(
    originalDocument: Document,
    documentPath: Path,
    documentName: String,
    pathResolver: ResourceResolver,
    validateDescriptor: Boolean
  ) {
    val pluginDescriptorParser = PluginDescriptorParser()
    val pluginBeanValidator = PluginBeanValidator()

    val validationContext = ValidationContext(descriptorPath, problemResolver)

    val parsingResult = pluginDescriptorParser.parse(
      descriptorPath,
      pluginFileName,
      originalDocument,
      documentPath,
      documentName,
      pathResolver,
      validationContext
    )
    if (parsingResult !is Parsed) {
      validationContext.problems.forEach { registerProblem(it) }
      return
    }
    val (document, bean) = parsingResult

    pluginBeanValidator.validate(bean, validationContext, validateDescriptor)
    val validationResult = validationContext.getResult {
      newInvalidPlugin(bean, document)
    }

    if (validationResult is ValidationResult.Invalid) {
      invalidPlugin = validationResult.invalidPlugin
      validationResult.problems.forEach { registerProblem(it) }
      return
    }
    if (validationResult is ValidationResult.ValidWithWarnings) {
      validationResult.warnings.forEach { registerProblem(it) }
    }

    plugin.underlyingDocument = document
    plugin.setInfoFromBean(bean, document)

    val themeResolution = themeLoader.load(plugin, documentPath, descriptorPath, pathResolver, ::registerProblem)
    when (themeResolution) {
      is PluginThemeLoader.Result.Found -> plugin.declaredThemes.addAll(themeResolution.themes)
      PluginThemeLoader.Result.NotFound -> return
    }

    validatePlugin(plugin)
  }


  internal fun registerProblem(problem: PluginProblem) {
    problems += problem
  }

  private fun PluginCreationResult<IdePlugin>.reassignStructureProblems() =
    when (this) {
      is PluginCreationSuccess -> copy(plugin = IdePluginImpl.clone(plugin, problems))
      is PluginCreationFail -> this
    }

  private fun PluginCreationResult<IdePlugin>.propagateResources() =
    when (this) {
      is PluginCreationSuccess -> copy(resources = this@PluginCreator.resources)
      is PluginCreationFail -> this
    }

  private val PluginCreationSuccess<IdePlugin>.problems: List<PluginProblem>
    get() = warnings + unacceptableWarnings

  private fun registerIfOptionalDependency(pluginDependency: PluginDependency, dependencyBean: PluginDependencyBean) {
    if (pluginDependency.isOptional && dependencyBean.configFile != null) {
      optionalDependenciesConfigFiles[pluginDependency] =
        if (v2ModulePrefix.matches(dependencyBean.configFile)) "../${dependencyBean.configFile}" else dependencyBean.configFile
    }
  }
}

private fun PluginCreationResult<IdePlugin>.add(telemetry: PluginTelemetry): PluginCreationResult<IdePlugin> {
  return when (this) {
    is PluginCreationSuccess -> this.copy(telemetry = telemetry)
    is PluginCreationFail -> this
  }
}
