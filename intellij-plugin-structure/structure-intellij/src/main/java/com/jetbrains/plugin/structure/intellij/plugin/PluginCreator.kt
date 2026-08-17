/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PluginProblem.Level.ERROR
import com.jetbrains.plugin.structure.base.problems.UnableToReadDescriptor
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
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.intellij.verifiers.*
import com.jetbrains.plugin.structure.intellij.xinclude.XIncluder
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

    // See shouldUsePlatformParser: plugins declaring since-build >= this baseline (262 = 2026.2)
    // get the plugin-system-parser-impl path; older plugins keep the JAXB path.
    private const val PLATFORM_PARSER_MIN_BASELINE = 262

    private val themeLoader = PluginThemeLoader()
    private val descriptorParser = PluginDescriptorParser()
    private val beanValidator = PluginBeanValidator()
    private val beanToPluginConverter = PluginBeanToIdePluginConverter()

    // POC: alternative pipeline backed by JetBrains' own plugin-system-parser-impl, selected per
    // plugin - see shouldUsePlatformParser and resolveDocumentAndValidateBean below.
    private val platformDescriptorParser = PlatformPluginDescriptorParser()
    private val platformDescriptorValidator = PlatformDescriptorValidator()
    private val rawDescriptorToPluginConverter = RawPluginDescriptorToIdePluginConverter()
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
    validateDescriptor: Boolean
  ) {
    val validationContext = ValidationContext(descriptorPath, problemResolver)

    // POC branch point: everything downstream of this `if` (theme loading, final structural
    // validation) is shared and unaware of which parser ran - both branches converge on the same
    // `plugin: IdePluginImpl` and both return `false` to signal "bail out, don't continue".
    // Per-plugin decision, not a global switch - see shouldUsePlatformParser below for why and how.
    val proceed = if (shouldUsePlatformParser(originalDocument)) {
      resolveDocumentAndValidateBeanViaPlatformParser(
        originalDocument, documentPath, documentName, pathResolver, validateDescriptor, validationContext
      )
    } else {
      resolveDocumentAndValidateBeanViaJaxb(
        originalDocument, documentPath, documentName, pathResolver, validateDescriptor, validationContext
      )
    }
    if (!proceed) {
      return
    }

    val themeResolution = themeLoader.load(plugin, documentPath, pathResolver, ::registerProblem)
    when (themeResolution) {
      is PluginThemeLoader.Result.Found -> plugin.declaredThemes.addAll(themeResolution.themes)
      PluginThemeLoader.Result.NotFound -> Unit
      PluginThemeLoader.Result.Failed -> return
    }

    validatePlugin(plugin)
  }

  /**
   * Per-plugin decision, per team discussion prior to this POC: "old path for old plugins, library
   * for 26.2 plugins" - the plugin's OWN declared minimum supported platform version decides which
   * parser it gets, not a single global switch for the whole process.
   *
   * Reads `since-build` directly off the raw, not-yet-fully-parsed [document] - cheap, and avoids a
   * chicken-and-egg problem: we don't yet know which of the two "full" parses to run, so we can't get
   * `since-build` from either of their outputs yet. `document.rootElement` is always available here -
   * it's the same JDOM `Document` [PluginBeanToIdePluginConverter] later reads unconditionally via
   * `document.rootElement` once one of the two parsers has run.
   */
  private fun shouldUsePlatformParser(document: Document): Boolean {
    val sinceBuild = document.rootElement.getChild("idea-version")?.getAttributeValue("since-build") ?: return false
    val parsedSinceBuild = IdeVersion.createIdeVersionIfValid(sinceBuild) ?: return false
    return parsedSinceBuild.baselineVersion >= PLATFORM_PARSER_MIN_BASELINE
  }

  /**
   * The JAXB pipeline: JDOM+JAXB via [PluginDescriptorParser]/[PluginBeanToIdePluginConverter]. Returns
   * a Boolean signaling whether the shared theme-loading/final-validation tail in
   * [resolveDocumentAndValidateBean] should run.
   */
  private fun resolveDocumentAndValidateBeanViaJaxb(
    originalDocument: Document,
    documentPath: Path,
    documentName: String,
    pathResolver: ResourceResolver,
    validateDescriptor: Boolean,
    validationContext: ValidationContext
  ): Boolean {
    val parsingResult = descriptorParser.parse(
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
      return false
    }
    val (document, bean) = parsingResult

    beanValidator.validate(bean, validationContext, validateDescriptor)
    val validationResult = validationContext.getResult {
      newInvalidPlugin(bean, document)
    }

    if (validationResult is ValidationResult.Invalid) {
      invalidPlugin = validationResult.invalidPlugin
      validationResult.problems.forEach { registerProblem(it) }
      return false
    }
    if (validationResult is ValidationResult.ValidWithWarnings) {
      validationResult.warnings.forEach { registerProblem(it) }
    }

    plugin.underlyingDocument = document
    beanToPluginConverter.convert(bean, document, parentPlugin, ::registerProblem, plugin)
    return true
  }

  /**
   * The platform-parser pipeline: JetBrains' own plugin-system-parser-impl via
   * [PlatformPluginDescriptorParser]/[RawPluginDescriptorToIdePluginConverter]. XIncludes are NOT
   * pre-resolved with [XIncluder] here - [PlatformPluginDescriptorParser] hands the ORIGINAL,
   * unresolved document straight to the new library, which resolves `<xi:include>` itself via
   * [ResourceRootXIncludeLoader]. See [PlatformPluginDescriptorParser]'s class doc for the corpus
   * scan that motivated this: real plugins do use plain `<xi:include>`, but not the `includeIf`/
   * `includeUnless`/`xpointer` features the new library has dropped, so letting it resolve includes
   * itself is both more representative of the new parser's actual behavior and, empirically,
   * unlikely to regress on that specific axis. `pathResolver`/`documentName` are consequently unused
   * on this path (both were only needed to drive [XIncluder]) - kept as parameters for symmetry with
   * [resolveDocumentAndValidateBeanViaJaxb] and because [resolveDocumentAndValidateBean] calls both
   * branches uniformly.
   *
   * The [RawPluginDescriptorToIdePluginConverter.convert] call is wrapped in try/catch, unlike the
   * JAXB path's equivalent call: content modules are resolved via this same [createPlugin] entry
   * point (from [com.jetbrains.plugin.structure.intellij.plugin.loaders.ModuleFromDescriptorLoader]),
   * so an uncaught exception here can escape all the way out of a content module's resolution and
   * kill the whole verifier worker process, not just fail one plugin. The JAXB converter can't throw
   * here because JDOM elements built by walking an already-parsed [Document] always have valid tag
   * names by construction; the new converter bridges the library's own lightweight element model into
   * JDOM by hand ([RawPluginDescriptorToIdePluginConverter.toJdomElement]), which is exactly the kind
   * of new, unproven code path this try/catch exists to contain.
   */
  @Suppress("UNUSED_PARAMETER")
  private fun resolveDocumentAndValidateBeanViaPlatformParser(
    originalDocument: Document,
    documentPath: Path,
    documentName: String,
    pathResolver: ResourceResolver,
    validateDescriptor: Boolean,
    validationContext: ValidationContext
  ): Boolean {
    val raw = platformDescriptorParser.parse(originalDocument, documentPath, descriptorPath, pluginFileName, validationContext)
    if (raw == null) {
      validationContext.problems.forEach { registerProblem(it) }
      return false
    }

    platformDescriptorValidator.validate(raw, validationContext, validateDescriptor)
    val validationResult = validationContext.getResult {
      newInvalidPlugin(raw, originalDocument)
    }

    if (validationResult is ValidationResult.Invalid) {
      invalidPlugin = validationResult.invalidPlugin
      validationResult.problems.forEach { registerProblem(it) }
      return false
    }
    if (validationResult is ValidationResult.ValidWithWarnings) {
      validationResult.warnings.forEach { registerProblem(it) }
    }

    // Still the UNRESOLVED document on this path - see PlatformPluginDescriptorParser's class doc,
    // point 4, on why that's a known, currently-cosmetic gap.
    plugin.underlyingDocument = originalDocument
    try {
      rawDescriptorToPluginConverter.convert(raw, originalDocument, parentPlugin, ::registerProblem, plugin)
    } catch (e: Throwable) {
      // See this method's doc for why this try/catch exists. Catches Throwable, not just Exception:
      // the platform parser's own Logger.error(...) throws AssertionError (an Error) for elements it
      // doesn't recognize - see PlatformPluginDescriptorParser.parse's matching catch for why.
      LOG.info("Unable to convert plugin descriptor $descriptorPath of $pluginFileName via platform parser", e)
      registerProblem(UnableToReadDescriptor(descriptorPath, e.localizedMessage))
      return false
    }
    return true
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
