/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PluginProblem.Level.ERROR
import com.jetbrains.plugin.structure.base.telemetry.MutablePluginTelemetry
import com.jetbrains.plugin.structure.base.telemetry.PluginTelemetry
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.intellij.plugin.PluginBeanToIdePluginConverter.UnsupportedClientAttributeValue
import com.jetbrains.plugin.structure.intellij.plugin.PluginDescriptorParser.ParseResult.Parsed
import com.jetbrains.plugin.structure.intellij.plugin.ValidationContext.ValidationResult
import com.jetbrains.plugin.structure.intellij.plugin.descriptors.DescriptorResource
import com.jetbrains.plugin.structure.intellij.plugin.loaders.PluginThemeLoader
import com.jetbrains.plugin.structure.intellij.problems.*
import com.jetbrains.plugin.structure.intellij.resources.PluginArchiveResource
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.verifiers.*
import org.jdom2.Document
import org.slf4j.LoggerFactory
import java.nio.file.Path

internal class PluginCreator private constructor(
  val pluginFileName: String,
  val descriptorPath: String,
  private val parentPlugin: PluginCreator?,
  private val problemResolver: PluginCreationResultResolver = IntelliJPluginCreationResultResolver()
) {

  companion object {
    private val LOG = LoggerFactory.getLogger(PluginCreator::class.java)

    val v2ModulePrefix = Regex("^intellij\\..*")

    private val themeLoader = PluginThemeLoader()
    private val descriptorParser = PluginDescriptorParser()
    private val beanValidator = PluginBeanValidator()
    private val beanToPluginConverter = PluginBeanToIdePluginConverter()
    private val legacyIntelliJIdeaPluginVerifier = LegacyIntelliJIdeaPluginVerifier()
    private val projectAndApplicationListenerAvailabilityVerifier = ProjectAndApplicationListenerAvailabilityVerifier()
    private val serviceExtensionPointPreloadVerifier = ServiceExtensionPointPreloadVerifier()
    private val statusBarWidgetFactoryExtensionPointVerifier = StatusBarWidgetFactoryExtensionPointVerifier()
    private val k2IdeModeCompatibilityVerifier = K2IdeModeCompatibilityVerifier()
    private val exposedModulesVerifier = ExposedModulesVerifier()

    @JvmStatic
    fun createPlugin(
      pluginFile: Path,
      descriptorPath: String,
      parentPlugin: PluginCreator?,
      validateDescriptor: Boolean,
      document: Document,
      documentPath: Path,
      pathResolver: ResourceResolver,
      mayHaveXIncludes: Boolean = true,
    ) = createPlugin(
      pluginFile.simpleName, descriptorPath, parentPlugin, validateDescriptor, document, documentPath, pathResolver, mayHaveXIncludes
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
      mayHaveXIncludes: Boolean = true,
    ): PluginCreator = createPlugin(
      pluginFileName, descriptorPath,
      parentPlugin, validateDescriptor,
      document, documentPath,
      pathResolver,
      IntelliJPluginCreationResultResolver(),
      mayHaveXIncludes
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
      problemResolver: PluginCreationResultResolver,
      mayHaveXIncludes: Boolean = true,
    ): PluginCreator {
      val pluginCreator = PluginCreator(pluginFileName, descriptorPath, parentPlugin, problemResolver)
      pluginCreator.resolveDocumentAndValidateBean(
        document, documentPath, descriptorPath, pathResolver, validateDescriptor, mayHaveXIncludes
      )
      return pluginCreator
    }

    @JvmStatic
    fun createPlugin(
      descriptorResource: DescriptorResource,
      parentPlugin: PluginCreator?,
      document: Document,
      pathResolver: ResourceResolver,
      problemResolver: PluginCreationResultResolver,
      mayHaveXIncludes: Boolean = true,
    ): PluginCreator {
      val pluginCreator =
        PluginCreator(descriptorResource.artifactFileName, descriptorResource.fileName, parentPlugin, problemResolver)
      pluginCreator.resolveDocumentAndValidateBean(
        document,
        descriptorResource.filePath,
        descriptorResource.fileName,
        pathResolver,
        validateDescriptor = true,
        mayHaveXIncludes = mayHaveXIncludes,
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

  private fun validatePlugin(plugin: IdePluginImpl) {
    val dependencies = plugin.dependencies
    dependencies.map { it.id }
      .groupingBy { it }
      .eachCount()
      .filterValues { it > 1 }
      .map { it.key }
      .forEach { duplicatedDependencyId -> registerProblem(DuplicatedDependencyWarning(duplicatedDependencyId)) }

    if (plugin.osConstraints.size > 1) {
      registerProblem(DependencyConstraintsDuplicates(
        descriptorPath = descriptorPath,
        modules = plugin.osConstraints.map { it.pluginAlias }
      ))
    }
    if (plugin.archConstraints.size > 1) {
      registerProblem(DependencyConstraintsDuplicates(
        descriptorPath = descriptorPath,
        modules = plugin.archConstraints.map { it.pluginAlias }
      ))
    }

    val sinceBuild = plugin.sinceBuild
    val untilBuild = plugin.untilBuild
    if (sinceBuild != null && untilBuild != null && sinceBuild > untilBuild) {
      registerProblem(SinceBuildGreaterThanUntilBuild(descriptorPath, sinceBuild, untilBuild))
    }

    legacyIntelliJIdeaPluginVerifier.verify(plugin, descriptorPath, ::registerProblem)
    projectAndApplicationListenerAvailabilityVerifier.verify(plugin, ::registerProblem)
    serviceExtensionPointPreloadVerifier.verify(plugin, ::registerProblem)
    statusBarWidgetFactoryExtensionPointVerifier.verify(plugin, ::registerProblem)
    k2IdeModeCompatibilityVerifier.verify(plugin, ::registerProblem, descriptorPath)
    exposedModulesVerifier.verify(plugin, ::registerProblem, descriptorPath)
  }

  private fun resolveDocumentAndValidateBean(
    originalDocument: Document,
    documentPath: Path,
    documentName: String,
    pathResolver: ResourceResolver,
    validateDescriptor: Boolean,
    mayHaveXIncludes: Boolean,
  ) {
    val validationContext = ValidationContext(descriptorPath, problemResolver)

    val parsingResult = descriptorParser.parse(
      descriptorPath,
      pluginFileName,
      originalDocument,
      documentPath,
      documentName,
      pathResolver,
      mayHaveXIncludes,
      validationContext
    )
    if (parsingResult !is Parsed) {
      validationContext.problems.forEach { registerProblem(it) }
      return
    }
    val (document, bean) = parsingResult

    beanValidator.validate(bean, validationContext, validateDescriptor)
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
    beanToPluginConverter.convert(bean, document, parentPlugin, ::registerProblem, plugin)

    val themeResolution = themeLoader.load(plugin, documentPath, pathResolver, ::registerProblem)
    when (themeResolution) {
      is PluginThemeLoader.Result.Found -> plugin.declaredThemes.addAll(themeResolution.themes)
      PluginThemeLoader.Result.NotFound -> Unit
      PluginThemeLoader.Result.Failed -> return
    }

    validatePlugin(plugin)
  }

  internal fun registerProblem(problem: PluginProblem) {
    problems += when (problem) {
      is UnsupportedClientAttributeValue -> UnknownServiceClientValue(descriptorPath, problem.unsupportedValue)
      else -> problem
    }
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
}

private fun PluginCreationResult<IdePlugin>.add(telemetry: PluginTelemetry): PluginCreationResult<IdePlugin> {
  return when (this) {
    is PluginCreationSuccess -> this.copy(telemetry = telemetry)
    is PluginCreationFail -> this
  }
}
