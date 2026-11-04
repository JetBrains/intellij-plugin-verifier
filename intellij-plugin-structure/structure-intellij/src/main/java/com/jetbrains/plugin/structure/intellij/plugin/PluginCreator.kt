/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PluginProblem.Level.ERROR
import com.jetbrains.plugin.structure.base.problems.UnableToReadDescriptor
import com.jetbrains.plugin.structure.base.telemetry.MutablePluginTelemetry
import com.jetbrains.plugin.structure.base.telemetry.PLUGIN_DESCRIPTOR_PARSER
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

// `<idea-version>` names read directly off the raw JDOM document, outside either full parse - by
// PluginCreator.shouldUsePlatformParser and PluginCreator.describePlatformParserSelection.
internal const val IDEA_VERSION_ELEMENT = "idea-version"
internal const val SINCE_BUILD_ATTRIBUTE = "since-build"
internal const val UNTIL_BUILD_ATTRIBUTE = "until-build"

internal class PluginCreator private constructor(
  val pluginFileName: String,
  val descriptorPath: String,
  private val parentPlugin: PluginCreator?,
  private val problemResolver: PluginCreationResultResolver = IntelliJPluginCreationResultResolver(),
  /**
   * Version of the IDE this descriptor is bundled in, or `null` for a plugin being verified rather than
   * loaded as part of an IDE. See [shouldUsePlatformParser], where it takes precedence over a bundled
   * descriptor's own compatibility declaration.
   */
  private val containingIdeVersion: IdeVersion? = null
) {

  companion object {
    private val LOG = LoggerFactory.getLogger(PluginCreator::class.java)

    /**
     * Deliberately its own logger, so that which-parser-ran can be switched on for a corpus run in
     * isolation - see [recordParserChoice].
     */
    private val PARSER_CHOICE_LOG = LoggerFactory.getLogger("com.jetbrains.plugin.structure.intellij.plugin.DescriptorParserChoice")

    /** Values of the [PLUGIN_DESCRIPTOR_PARSER] telemetry key. Kept stable: corpus runs count these. */
    private const val PLATFORM_PARSER_NAME = "platform"
    private const val JAXB_PARSER_NAME = "jaxb"

    val v2ModulePrefix = Regex("^intellij\\..*")

    /**
     * Baseline of the release in which `includeIf`/`includeUnless` are planned for removal from the
     * platform (IJPL-215563, 26.3). The parser library already rejects them for all but an internal
     * allowlist of plugin IDs; a plugin declaring compatibility at or past this baseline is expected
     * not to rely on them, which is what makes it acceptable to hand it to that parser.
     */
    private const val CONDITIONAL_INCLUDE_REMOVAL_BASELINE = 263

    /**
     * Trust floor for a descriptor that declares no upper bound. An unbounded `until-build` claims
     * compatibility with every future IDE, including post-removal ones; that claim is only meaningful
     * if the plugin was built recently enough for the author to have considered it.
     */
    private const val UNBOUNDED_UNTIL_SINCE_FLOOR = 252

    private val themeLoader = PluginThemeLoader()
    private val descriptorParser = PluginDescriptorParser()
    private val descriptorValidator = PluginDescriptorValidator()
    private val beanToPluginConverter = PluginBeanToIdePluginConverter()

    // Alternative pipeline backed by JetBrains' own plugin-system-parser-impl, selected once per
    // plugin - see shouldUsePlatformParser and resolveDocumentAndValidateBean below.
    private val platformDescriptorParser = PlatformPluginDescriptorParser()
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
    @JvmOverloads
    fun createPlugin(
      pluginFileName: String,
      descriptorPath: String,
      parentPlugin: PluginCreator?,
      validateDescriptor: Boolean,
      document: Document,
      documentPath: Path,
      pathResolver: ResourceResolver,
      problemResolver: PluginCreationResultResolver,
      containingIdeVersion: IdeVersion? = null
    ): PluginCreator {
      val pluginCreator =
        PluginCreator(pluginFileName, descriptorPath, parentPlugin, problemResolver, containingIdeVersion)
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
        document, descriptorResource.filePath, descriptorResource.fileName, pathResolver,
        validateDescriptor = true,
        // An inline content module has no filesystem path of its own, and - unlike a file-based
        // descriptor - IntelliJ's own loader does not give it XInclude support either: embedded module
        // content is parsed with a null XIncludeLoader (PluginDescriptorLoader.loadPluginSubDescriptors).
        // ResourceRootSource.ContainingDescriptor therefore withholds the resource root, so on the
        // platform-parser path an <xi:include> here fails the parse instead of being resolved. The JAXB
        // path is unaffected and still runs XIncluder. The parser choice itself is inherited via
        // parentPlugin regardless (see shouldUsePlatformParser).
        resourceRootSource = ResourceRootSource.ContainingDescriptor
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

  /**
   * Which of the two parsers ran for this descriptor. Nested descriptors read it in
   * [shouldUsePlatformParser], so the choice is made once per plugin, at its main descriptor, and
   * inherited by its content modules, V2 module descriptors and `<depends config-file="...">` descriptors.
   *
   * `false` until [resolveDocumentAndValidateBean] has run, which is always before any nested
   * descriptor is created: [com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager] resolves
   * content modules and optional dependencies only after the enclosing [createPlugin] has returned.
   */
  internal var usedPlatformParser: Boolean = false
    private set

  /**
   * Why [usedPlatformParser] is set, see [describePlatformParserSelection]. Inherited like the choice
   * itself, and `null` on the JAXB path.
   */
  private var platformParserReason: String? = null

  /**
   * The root a classloader would resolve this descriptor's resources against - what the platform
   * parser's already-joined `<xi:include>` paths (e.g. `"META-INF/extensions.xml"`) are relative to.
   * See [resolveResourceRoot].
   *
   * `null`, deliberately, for an inline content module ([ResourceRootSource.ContainingDescriptor]):
   * `<xi:include>` is then unsupported for it, matching IntelliJ's own loader, which parses embedded
   * module content with a null `XIncludeLoader`. On the platform-parser path, an include with no
   * resource root fails the parse rather than resolving against a root the real IDE would not honour.
   */
  internal var resourceRoot: Path? = null
    private set

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
    val dependencies = plugin.reconstructAllDependencies(removeDuplicates = false)
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
    resourceRootSource: ResourceRootSource = ResourceRootSource.OwnDocumentPath
  ) {
    val validationContext = ValidationContext(descriptorPath, problemResolver)

    resourceRoot = resolveResourceRoot(documentPath, resourceRootSource)
    usedPlatformParser = shouldUsePlatformParser(originalDocument)
    platformParserReason = if (usedPlatformParser) describePlatformParserSelection(originalDocument) else null
    recordParserChoice()

    // Everything after this `if` (theme loading, final structural validation) is shared and unaware of
    // which parser ran: both branches fill the same `plugin` and return `false` to bail out.
    val proceed = if (usedPlatformParser) {
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
   * Reports which parser this descriptor got, on two channels, because neither alone covers the whole
   * corpus:
   *
   *  - [telemetry], under [PLUGIN_DESCRIPTOR_PARSER]. Structured and written out by the verifier's
   *    reportage, but attached to a [PluginCreationSuccess] only - a plugin that fails to build carries
   *    no telemetry, and those are the plugins a parser migration most needs to attribute.
   *  - [PARSER_CHOICE_LOG], at DEBUG, which covers those failures. It is a separate logger so that a
   *    corpus run can turn on just this, without the rest of [PluginCreator] at DEBUG.
   *
   * Only the top-level creator's telemetry is reported; since the choice is made once per plugin and
   * inherited, that is one record per decision.
   */
  private fun recordParserChoice() {
    val parser = if (usedPlatformParser) PLATFORM_PARSER_NAME else JAXB_PARSER_NAME
    telemetry[PLUGIN_DESCRIPTOR_PARSER] = parser
    if (PARSER_CHOICE_LOG.isDebugEnabled) {
      PARSER_CHOICE_LOG.debug(
        "Parsed descriptor '{}' of '{}' with the {} parser", descriptorPath, pluginFileName, parser
      )
    }
  }

  /**
   * Whether this descriptor should be parsed by the platform parser rather than the JAXB pipeline.
   *
   * The platform parser is opt-in: unless [Settings.PLATFORM_PARSER_ENABLED] is set, every descriptor
   * goes through the JAXB pipeline and the rules below are not consulted.
   *
   * Nested descriptors inherit their enclosing plugin's choice. A top-level descriptor loaded through
   * a bundled API is selected by the containing IDE version, regardless of the descriptor's own
   * compatibility declaration. Only standalone plugins are selected from their declared compatibility
   * range.
   *
   * The platform parser rejects `includeIf`/`includeUnless` for non-allowlisted plugins (see
   * [com.jetbrains.plugin.structure.intellij.problems.ConditionalIncludeNotSupported]), which the JAXB
   * path does not. Handing a standalone plugin to it is therefore only safe once the plugin cannot be
   * relying on those attributes - i.e. once its declared compatibility interval overlaps the IDEs they
   * are removed from, `[CONDITIONAL_INCLUDE_REMOVAL_BASELINE, infinity)`.
   *
   * Overlap with a half-open upper interval only constrains the upper bound, so the decision is driven
   * by `until-build` alone; `since-build` only serves as the trust floor for the unbounded case, where
   * an omitted `until-build` claims compatibility with every future IDE and that claim is worth
   * something only if the plugin is recent enough for its author to have meant it.
   *
   * Both attributes are read directly off the raw [document]: which full parse to run is not known
   * yet, so neither parse's output is available.
   *
   * Notes on the comparisons:
   *
   * - Comparing baselines is sufficient. `IdeVersionImpl.fromString` reduces `262.*` to baseline 262
   *   with snapshot components, strips a product code (`IU-262.*` -> 262), and treats a bare `262` as a
   *   baseline rather than a build number.
   * - An unparseable `until-build` deliberately falls through to the `since-build` clause: a malformed
   *   upper bound is not a declaration of compatibility. Selection runs before validation and must not
   *   depend on it - a malformed bound is reported separately, by [PluginDescriptorValidator] (only when
   *   `validateDescriptor` is set).
   * - Content modules, V2 module descriptors and `<depends config-file="...">` descriptors carry no
   *   `<idea-version>` of their own, so evaluating the rule against them would split a single plugin
   *   across both parsers; they inherit instead. See [usedPlatformParser].
   */
  internal fun shouldUsePlatformParser(document: Document): Boolean {
    if (!Settings.PLATFORM_PARSER_ENABLED.getAsBoolean()) return false
    parentPlugin?.let { return it.usedPlatformParser }
    containingIdeVersion?.let {
      return it.baselineVersion >= CONDITIONAL_INCLUDE_REMOVAL_BASELINE
    }

    val ideaVersion = document.rootElement.getChild(IDEA_VERSION_ELEMENT)
    val untilBuild = ideaVersion?.getAttributeValue(UNTIL_BUILD_ATTRIBUTE)
      ?.let { IdeVersion.createIdeVersionIfValid(it) }
    if (untilBuild != null) {
      return untilBuild.baselineVersion >= CONDITIONAL_INCLUDE_REMOVAL_BASELINE
    }
    val sinceBuild = ideaVersion?.getAttributeValue(SINCE_BUILD_ATTRIBUTE)
      ?.let { IdeVersion.createIdeVersionIfValid(it) }
    if (sinceBuild != null) {
      return sinceBuild.baselineVersion >= UNBOUNDED_UNTIL_SINCE_FLOOR
    }
    return false
  }

  /**
   * Why [shouldUsePlatformParser] selected the platform parser, worded to follow "this plugin" in
   * [com.jetbrains.plugin.structure.intellij.problems.ConditionalIncludeNotSupported]'s message: the
   * enclosing plugin's reason for a nested descriptor, the containing IDE for a bundled one, and the
   * declared `<idea-version>` range otherwise.
   */
  private fun describePlatformParserSelection(document: Document): String {
    parentPlugin?.platformParserReason?.let { return it }
    containingIdeVersion?.let { return "is bundled in IDE $it" }

    val ideaVersion = document.rootElement.getChild(IDEA_VERSION_ELEMENT)
    val since = ideaVersion?.getAttributeValue(SINCE_BUILD_ATTRIBUTE)
    val until = ideaVersion?.getAttributeValue(UNTIL_BUILD_ATTRIBUTE)
    return when {
      since != null && until != null -> "declares compatibility with IDEs from $since up to $until"
      since != null -> "declares compatibility with IDEs from $since onwards, with no upper bound"
      until != null -> "declares compatibility with IDEs up to $until"
      else -> "declares no <idea-version> range"
    }
  }

  /**
   * Derives the resource root a classloader would use for this descriptor - what the platform parser's
   * already-joined `<xi:include>` paths (e.g. `"META-INF/extensions.xml"`) are relative to, see
   * [com.jetbrains.plugin.structure.intellij.xinclude.ResourceResolverXIncludeLoader].
   *
   * [documentPath] is absolutised first, and that is load-bearing: a descriptor inside a JAR is
   * addressed by a path that is *relative* within the ZIP filesystem (`META-INF/plugin.xml`), so its
   * `parent.parent` is `null`. Absolutised, it becomes `/META-INF/plugin.xml`.
   *
   * The root is then the parent of the nearest `META-INF` ancestor, and `parent` if there is none:
   *
   *  - The main `plugin.xml`, and V1 `<depends optional="true" config-file="...">` descriptors with a
   *    plain file name, live directly under `<root>/META-INF/` (`JarPluginLoader` and
   *    `PluginDirectoryLoader` both prefix the descriptor path with `META-INF/`). Root is the ZIP root
   *    `/`, or the exploded plugin directory.
   *  - File-based content modules ([PluginModuleResolver]'s `"../$moduleName.xml"` convention), and
   *    config-files named `intellij.*` (which [DependsPluginDependency.resolveDescriptorPath] also
   *    prefixes with `../`), land at the root of whichever JAR or directory holds them: the main
   *    artifact, `lib/modules/<name>.jar`, or any JAR in `lib` searched by `LibDirectoryPluginLoader`.
   *    Here `parent` is the root.
   *  - A config-file in a subdirectory (`META-INF/sub/x.xml`) still gets the plugin's root, as in
   *    IntelliJ, which resolves its includes against the plugin classloader.
   *
   * [ResourceRootSource.ContainingDescriptor] covers an inline content module
   * (`<module name="...">CDATA</module>`), which has no filesystem path: `DescriptorResource`
   * synthesises [documentPath] as a bare single-segment path, so absolutising it would root the module
   * at the current working directory. Such a module gets `null`, deliberately, not the parent's root:
   * IntelliJ's own loader parses embedded module content with a null `XIncludeLoader`
   * (`PluginDescriptorLoader.loadPluginSubDescriptors`), so an inherited root would let an include
   * resolve here that the real IDE rejects. The parser choice is still inherited, see
   * [shouldUsePlatformParser].
   */
  private fun resolveResourceRoot(documentPath: Path, resourceRootSource: ResourceRootSource): Path? {
    if (resourceRootSource == ResourceRootSource.ContainingDescriptor) {
      return null
    }
    val parent = documentPath.toAbsolutePath().parent ?: return parentPlugin?.resourceRoot
    val metaInf = generateSequence(parent) { it.parent }.firstOrNull { it.fileName?.toString() == IdePluginManager.META_INF }
    return metaInf?.parent ?: parent
  }

  /**
   * How a descriptor's resource root is obtained, see [resolveResourceRoot].
   */
  private enum class ResourceRootSource {
    /** Derived from the descriptor's own path within its artifact. */
    OwnDocumentPath,

    /**
     * An inline content module, whose content is embedded in the containing descriptor. Despite the
     * name, nothing is taken from that descriptor: the result is no resource root at all, since IntelliJ's
     * own loader gives embedded module content no XInclude support either. See [resolveResourceRoot].
     */
    ContainingDescriptor
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

    descriptorValidator.validate(PluginBeanView(bean), validationContext, validateDescriptor)
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
   * pre-resolved with [XIncluder] here: the library resolves `<xi:include>` in the ORIGINAL document
   * itself, through [com.jetbrains.plugin.structure.intellij.xinclude.ResourceResolverXIncludeLoader]
   * over this [pathResolver] anchored at [resourceRoot] - including includes whose target lives in a
   * sibling JAR of the plugin's `lib` directory. Beyond [XIncluder]'s own fallback lookups, the library
   * differs in two features it has dropped: `includeIf`/`includeUnless`
   * (see [com.jetbrains.plugin.structure.intellij.problems.ConditionalIncludeNotSupported]) and
   * non-default `xpointer` values. `documentPath` and `documentName` are therefore unused here; they
   * are kept so that [resolveDocumentAndValidateBean] can call both branches uniformly.
   *
   * The [RawPluginDescriptorToIdePluginConverter.convert] call is wrapped in try/catch, unlike the
   * JAXB path's converter call, so that a conversion failure (e.g. a malformed `since-build` that
   * validation did not reject, or a bad element name in the hand-written
   * [RawPluginDescriptorToIdePluginConverter.toJdomElement] bridge) becomes an [UnableToReadDescriptor]
   * for this descriptor. Inline content modules reach this method via
   * [com.jetbrains.plugin.structure.intellij.plugin.loaders.ModuleFromDescriptorLoader], which does
   * not catch it.
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
    val parseResult = platformDescriptorParser.parse(
      originalDocument, resourceRoot, pathResolver, descriptorPath, pluginFileName,
      checkNotNull(platformParserReason), validationContext
    )
    if (parseResult == null) {
      validationContext.problems.forEach { registerProblem(it) }
      return false
    }
    val raw = parseResult.descriptor

    descriptorValidator.validate(parseResult.view, validationContext, validateDescriptor)
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

    // Still the UNRESOLVED document on this path - see PlatformPluginDescriptorParser's class doc, point 4.
    plugin.underlyingDocument = originalDocument
    try {
      rawDescriptorToPluginConverter.convert(raw, parseResult.view, originalDocument, parentPlugin, ::registerProblem, plugin)
    } catch (e: Exception) {
      // Contain descriptor/conversion failures, but let JVM-fatal errors and linkage failures escape.
      // Platform-logger AssertionError handling belongs at the parser boundary.
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
