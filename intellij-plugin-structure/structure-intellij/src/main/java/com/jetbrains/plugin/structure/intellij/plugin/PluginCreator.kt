/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.intellij.beans.*
import com.jetbrains.plugin.structure.intellij.extractor.PluginBeanExtractor
import com.jetbrains.plugin.structure.intellij.problems.*
import com.jetbrains.plugin.structure.intellij.problems.TooLongPropertyValue
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
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
  private val parentPlugin: PluginCreator?
) {

  companion object {
    private val LOG = LoggerFactory.getLogger(PluginCreator::class.java)

    private const val MAX_PRODUCT_CODE_LENGTH = 15
    private const val MAX_VERSION_LENGTH = 64
    private const val MAX_PROPERTY_LENGTH = 255
    private const val MAX_LONG_PROPERTY_LENGTH = 65535

    private const val INTELLIJ_THEME_EXTENSION = "com.intellij.themeProvider"

    val v2ModulePrefix = Regex("^intellij\\..*")

    private val latinSymbolsRegex = Regex("[A-Za-z]|\\s")

    private val json = jacksonObjectMapper()

    private val releaseDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

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
    ): PluginCreator {
      val pluginCreator = PluginCreator(pluginFileName, descriptorPath, parentPlugin)
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
      require(singleProblem.level == PluginProblem.Level.ERROR) { "Only ERROR problems are allowed here" }
      val pluginCreator = PluginCreator(pluginFileName, descriptorPath, null)
      pluginCreator.registerProblem(singleProblem)
      return pluginCreator
    }
  }

  val optionalDependenciesConfigFiles: MutableMap<PluginDependency, String> = linkedMapOf()

  private val plugin = IdePluginImpl()
  private val problems = arrayListOf<PluginProblem>()

  val pluginId: String?
    get() = plugin.pluginId ?: parentPlugin?.pluginId

  val isSuccess: Boolean
    get() = !hasErrors()

  val pluginCreationResult: PluginCreationResult<IdePlugin>
    get() = if (hasErrors()) {
      PluginCreationFail(problems)
    } else {
      PluginCreationSuccess<IdePlugin>(plugin, problems)
    }

  fun addOptionalDescriptor(
    pluginDependency: PluginDependency,
    configurationFile: String,
    optionalDependencyCreator: PluginCreator
  ) {
    val pluginCreationResult = optionalDependencyCreator.pluginCreationResult
    if (pluginCreationResult is PluginCreationSuccess<IdePlugin>) {
      val optionalPlugin = pluginCreationResult.plugin
      plugin.optionalDescriptors += OptionalPluginDescriptor(pluginDependency, optionalPlugin, configurationFile)
      optionalPlugin.extensions.forEach { (extensionPointName, extensionElement) ->
        plugin.extensions.getOrPut(extensionPointName) { arrayListOf() }.addAll(extensionElement)
      }
      if (optionalPlugin is IdePluginImpl) {
        plugin.appContainerDescriptor.mergeWith(optionalPlugin.appContainerDescriptor)
        plugin.projectContainerDescriptor.mergeWith(optionalPlugin.projectContainerDescriptor)
        plugin.moduleContainerDescriptor.mergeWith(optionalPlugin.moduleContainerDescriptor)
      }
    } else {
      val errors = (pluginCreationResult as PluginCreationFail<IdePlugin>)
        .errorsAndWarnings
        .filter { e -> e.level === PluginProblem.Level.ERROR }
      registerProblem(OptionalDependencyDescriptorResolutionProblem(pluginDependency.id, configurationFile, errors))
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

  fun setPluginVersion(pluginVersion: String) {
    plugin.pluginVersion = pluginVersion
  }

  fun setOriginalFile(originalFile: Path) {
    plugin.originalFile = originalFile
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
        if (dependencyBeanV2.dependencyId != null) {
          val dependency = PluginDependencyImpl(dependencyBeanV2.dependencyId, false, false)
          //TODO: get dependencies from dependency config file
          dependencies += PluginDependencyImpl("unresolved", true, false)
          optionalDependenciesConfigFiles[dependency] = "../${dependencyBeanV2.dependencyId.replace("/", ".")}.xml"
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

    if (bean.contentDependencies != null) {
      for (dependencyBeanContent in bean.contentDependencies.modules) {
        if (dependencyBeanContent.dependencyId != null) {
          val dependency = PluginDependencyImpl(dependencyBeanContent.dependencyId, true, false)
          //TODO: get dependencies from dependency config file
          dependencies += PluginDependencyImpl("unresolved", true, false)
          optionalDependenciesConfigFiles[dependency] = "../${dependencyBeanContent.dependencyId.replace("/", ".")}.xml"
        }
      }
      //TODO: is this even possible?
      for (dependencyBeanContent in bean.contentDependencies.plugins) {
        if (dependencyBeanContent.dependencyId != null) {
          val dependency = PluginDependencyImpl(dependencyBeanContent.dependencyId, true, false)
          dependencies += dependency

          // TODO: understand how optional dependencies config files work in new format
//          if (dependency.isOptional && dependencyBean.configFile != null) {
//            optionalDependenciesConfigFiles[dependency] = dependencyBean.configFile
//          }
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
        Integer.parseInt(productDescriptorBean.releaseVersion),
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
          "com.intellij.applicationService" -> idePlugin.appContainerDescriptor.services += readServiceDescriptor(extensionElement)
          "com.intellij.projectService" -> idePlugin.projectContainerDescriptor.services += readServiceDescriptor(extensionElement)
          "com.intellij.moduleService" -> idePlugin.moduleContainerDescriptor.services += readServiceDescriptor(extensionElement)
          else -> idePlugin.extensions.getOrPut(epName) { arrayListOf() }.add(extensionElement)
        }
      }
    }
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

  private fun readServiceDescriptor(extensionElement: Element): IdePluginContentDescriptor.ServiceDescriptor {
    val serviceInterface = extensionElement.getAttributeValue("serviceInterface")
    val serviceImplementation = extensionElement.getAttributeValue("serviceImplementation")
    return IdePluginContentDescriptor.ServiceDescriptor(serviceInterface, serviceImplementation)
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

  private fun readListeners(rootElement: Element, listenersName: String, containerDescriptor: MutableIdePluginContentDescriptor) {
    for (listenersRoot in rootElement.getChildren(listenersName)) {
      for (listener in listenersRoot.children) {
        val className = listener.getAttributeValue("class")
        val topicName = listener.getAttributeValue("topic")
        if (className == null) {
          registerProblem(ElementMissingAttribute("listener", "class"))
        }
        if (topicName == null) {
          registerProblem(ElementMissingAttribute("listener", "topic"))
        }
        if (className != null && topicName != null) {
          containerDescriptor.listeners += IdePluginContentDescriptor.ListenerDescriptor(topicName, className)
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

  private fun validatePluginBean(bean: PluginBean, validateDescriptor: Boolean) {
    if (validateDescriptor || bean.url != null) {
      validateBeanUrl(bean.url)
    }
    if (validateDescriptor || bean.id != null) {
      validateId(bean.id)
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
    }
    if (validateDescriptor || bean.productDescriptor != null) {
      validateProductDescriptor(bean.productDescriptor)
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
      } else if (dependencyBean.optional == true && dependencyBean.configFile == null) {
        registerProblem(OptionalDependencyConfigFileNotSpecified(dependencyBean.dependencyId))
      } else if (dependencyBean.optional == false) {
        registerProblem(SuperfluousNonOptionalDependencyDeclaration(dependencyBean.dependencyId))
      }
    }
  }

  private fun validateProductDescriptor(productDescriptor: ProductDescriptorBean?) {
    if (productDescriptor != null) {
      validateProductCode(productDescriptor.code)
      validateReleaseDate(productDescriptor.releaseDate)
      validateReleaseVersion(productDescriptor.releaseVersion)
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
        LocalDate.parse(releaseDate, releaseDateFormatter)
      } catch (e: DateTimeParseException) {
        registerProblem(ReleaseDateWrongFormat)
      }
    }
  }

  private fun validateReleaseVersion(releaseVersion: String?) {
    if (releaseVersion.isNullOrEmpty()) {
      registerProblem(PropertyNotSpecified("release-version", descriptorPath))
    } else {
      try {
        Integer.valueOf(releaseVersion)
      } catch (e: NumberFormatException) {
        registerProblem(NotNumber("release-version", descriptorPath))
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
    dependencies.map { it.id }
      .groupingBy { it }
      .eachCount()
      .filterValues { it > 1 }
      .map { it.key }
      .forEach { duplicatedDependencyId -> registerProblem(DuplicatedDependencyWarning(duplicatedDependencyId)) }

    if (dependencies.count { it.isModule } == 0) {
      registerProblem(NoModuleDependencies(descriptorPath))
    }

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
    if (hasErrors()) {
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
            registerProblem(UnableToReadTheme(descriptorPath, themePath, e.localizedMessage))
            return null
          }
          themes.add(theme)
        }
        is ResourceResolver.Result.NotFound -> {
          registerProblem(UnableToFindTheme(descriptorPath, themePath))
        }
        is ResourceResolver.Result.Failed -> {
          registerProblem(UnableToReadTheme(descriptorPath, themePath, resolvedTheme.exception.localizedMessage))
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

  private fun registerProblem(problem: PluginProblem) {
    problems += problem
  }

  private fun hasErrors() = problems.any { it.level === PluginProblem.Level.ERROR }

  private fun validateId(id: String?) {
    if (id != null) {
      when {
        id.isBlank() -> {
          registerProblem(PropertyNotSpecified("id"))
        }
        "com.your.company.unique.plugin.id" == id -> {
          registerProblem(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.ID))
        }
        else -> {
          validatePropertyLength("id", id, MAX_PROPERTY_LENGTH)
          validateNewlines("id", id)
        }
      }
    }
  }

  private fun validateName(name: String?) {
    when {
      name.isNullOrBlank() -> registerProblem(PropertyNotSpecified("name", descriptorPath))
      "Plugin display name here" == name -> registerProblem(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.NAME))
      else -> {
        val templateWords = listOf("plugin", "JetBrains", "IntelliJ")
        val templateWord = templateWords.find { name.contains(it, true) }
        if (templateWord != null) {
          registerProblem(TemplateWordInPluginName(descriptorPath, templateWord))
        }
        validatePropertyLength("name", name, MAX_NAME_LENGTH)
        validateNewlines("name", name)
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

    val textDescription = Jsoup.parseBodyFragment(htmlDescription).text()

    if (textDescription.length < 40) {
      registerProblem(ShortDescription())
      return
    }

    if (textDescription.contains("Enter short description for your plugin here.") || textDescription.contains("most HTML tags may be used")) {
      registerProblem(DefaultDescription(descriptorPath))
      return
    }

    val latinSymbols = latinSymbolsRegex.findAll(textDescription).count()
    if (latinSymbols < 40) {
      registerProblem(NonLatinDescription())
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

  private fun validateNewlines(propertyName: String, propertyValue: String) {
    if (propertyValue.trim().contains("\n")) {
      registerProblem(ContainsNewlines(propertyName, descriptorPath))
    }
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

    if (vendorBean.url.isNullOrBlank() && vendorBean.email.isNullOrBlank() && vendorBean.name.isNullOrBlank()) {
      registerProblem(PropertyNotSpecified("vendor", descriptorPath))
      return
    }

    if ("YourCompany" == vendorBean.name) {
      registerProblem(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.VENDOR))
    }
    validatePropertyLength("vendor", vendorBean.name, MAX_PROPERTY_LENGTH)

    if ("https://www.yourcompany.com" == vendorBean.url) {
      registerProblem(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.VENDOR_URL))
    }
    validatePropertyLength("vendor url", vendorBean.url, MAX_PROPERTY_LENGTH)

    if ("support@yourcompany.com" == vendorBean.email) {
      registerProblem(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.VENDOR_EMAIL))
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
        if (sinceBuildParsed.baselineVersion < 130 && sinceBuild.endsWith(".*")) {
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

  private fun validateUntilBuild(untilBuild: String) {
    val untilBuildParsed = IdeVersion.createIdeVersionIfValid(untilBuild)
    if (untilBuildParsed == null) {
      registerProblem(InvalidUntilBuild(descriptorPath, untilBuild))
    } else {
      if (untilBuildParsed.baselineVersion > 999) {
        registerProblem(ErroneousUntilBuild(descriptorPath, untilBuildParsed))
      } else if (untilBuildParsed.baselineVersion > 400) {
        registerProblem(SuspiciousUntilBuild(untilBuild))
      }
      if (untilBuildParsed.productCode.isNotEmpty()) {
        registerProblem(ProductCodePrefixInBuild(descriptorPath))
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

    val untilBuild = versionBean.untilBuild
    if (untilBuild != null) {
      validateUntilBuild(untilBuild)
    }
  }

}
