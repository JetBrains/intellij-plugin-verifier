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

/** Descriptor element and attribute names read before either full parse has run. */
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
   * loaded as part of an IDE. See [shouldUsePlatformParser], which falls back to it.
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
     * Baseline at which `includeIf`/`includeUnless` are removed from the platform (IJPL-215563, 26.3).
     * A plugin declaring compatibility at or past this point cannot rely on them, which is what makes
     * it safe to hand to a parser that rejects them.
     */
    private const val CONDITIONAL_INCLUDE_REMOVAL_BASELINE = 263

    /**
     * Trust floor for a descriptor that declares no upper bound. An unbounded `until-build` claims
     * compatibility with every future IDE, including post-removal ones; that claim is only meaningful
     * if the plugin was built recently enough for the author to have considered it.
     */
    private const val UNBOUNDED_UNTIL_SINCE_FLOOR = 252

    /**
     * Evaluation switch: when `true`, every descriptor goes to the platform parser regardless of what
     * [shouldUsePlatformParser] says, so that a corpus run exercises it everywhere - including the
     * plugins with the large, `xi:include`-heavy descriptors the rule leaves on the JAXB path. Flipping
     * it is a one-line change, and the rule is evaluated on every descriptor either way, so it cannot
     * rot while overridden.
     *
     * Left `false` by default. PARSER_PLAN.md Step 6 asked for `true`, on the grounds that the rule
     * selects no plugin using `xi:include` and the XInclude rework would otherwise have no coverage at
     * all - but Step 7's fixtures now provide exactly that coverage: every descriptor in
     * `PlatformParserXIncludeTest` declares `until-build="263.*"`, so the rule selects it on its own
     * merits. With the fixtures carrying coverage, `true` buys corpus breadth rather than basic
     * coverage, and costs a red test suite: it re-exposes the validation-parity and library-strictness
     * divergences that Step 7 equally requires to stay green (PARSER_PLAN.md, "Flagged, not decided").
     */
    private const val FORCE_PLATFORM_PARSER_FOR_EVERY_DESCRIPTOR = false

    private val themeLoader = PluginThemeLoader()
    private val descriptorParser = PluginDescriptorParser()
    private val beanValidator = PluginBeanValidator()
    private val beanToPluginConverter = PluginBeanToIdePluginConverter()

    // Alternative pipeline backed by JetBrains' own plugin-system-parser-impl, selected once per
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
        // An inline content module has no filesystem path of its own to derive a resource root from,
        // so it resolves its includes against the artifact its containing descriptor came from.
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
   * Which of the two parsers actually ran for this descriptor. Read by [shouldUsePlatformParser] of
   * every descriptor nested in this one, so that the choice is made once per plugin - at its main
   * descriptor, the only one carrying an `<idea-version>` - and inherited by its content modules, V2
   * module descriptors and `<depends config-file="...">` descriptors.
   *
   * `false` until [resolveDocumentAndValidateBean] has run, which is always before any nested
   * descriptor is created: content modules and optional dependencies are resolved by
   * [com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager] only after the enclosing
   * [createPlugin] has returned.
   */
  internal var usedPlatformParser: Boolean = false
    private set

  /**
   * The root a classloader would resolve this descriptor's resources against - what the platform
   * parser's already-joined `<xi:include>` paths (e.g. `"META-INF/extensions.xml"`) are relative to.
   * See [resolveResourceRoot].
   *
   * `null` for a descriptor that has neither a usable filesystem path of its own nor a parent to
   * inherit one from; `<xi:include>` is then unsupported for it, and encountering one fails the parse
   * rather than resolving against a guessed-wrong root.
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
    resourceRootSource: ResourceRootSource = ResourceRootSource.OwnDocumentPath
  ) {
    val validationContext = ValidationContext(descriptorPath, problemResolver)

    resourceRoot = resolveResourceRoot(documentPath, resourceRootSource)
    // Evaluated unconditionally, before the `||`, so that the rule keeps running - and keeps being
    // exercised - even while FORCE_PLATFORM_PARSER_FOR_EVERY_DESCRIPTOR overrides its verdict.
    usedPlatformParser = shouldUsePlatformParser(originalDocument) || FORCE_PLATFORM_PARSER_FOR_EVERY_DESCRIPTOR
    recordParserChoice()

    // Branch point: everything downstream of this `if` (theme loading, final structural validation)
    // is shared and unaware of which parser ran - both branches converge on the same
    // `plugin: IdePluginImpl` and both return `false` to signal "bail out, don't continue".
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
   *  - [telemetry], under [PLUGIN_DESCRIPTOR_PARSER]. Structured and already plumbed to disk by the
   *    verifier's reportage, but attached to a [PluginCreationSuccess] only - a plugin that fails to
   *    build carries no telemetry at all, and those are exactly the plugins a parser migration most
   *    needs to attribute.
   *  - [PARSER_CHOICE_LOG], at DEBUG. Covers the failures the telemetry channel drops. It is a separate
   *    logger from this class's own so that a corpus run can turn just this on, without the rest of
   *    [PluginCreator] at DEBUG.
   *
   * Only the top-level creator's telemetry is reported, which is the right granularity: the choice is
   * made once per plugin and inherited, so one record per plugin is one record per decision.
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
   * The platform parser rejects `includeIf`/`includeUnless` (see
   * [com.jetbrains.plugin.structure.intellij.problems.ConditionalIncludeNotSupported]), which the JAXB
   * path still honours. Handing a plugin to it is therefore only safe once the plugin itself cannot be
   * relying on those attributes - and what settles that is the plugin's declared compatibility
   * *interval* overlapping the range of IDEs the attributes no longer exist in,
   * `[CONDITIONAL_INCLUDE_REMOVAL_BASELINE, infinity)`.
   *
   * Overlap with a half-open upper interval only ever constrains the upper bound, so the decision is
   * driven by `until-build` alone; `since-build` survives only as the trust floor for the unbounded
   * case, where an omitted `until-build` claims compatibility with every future IDE and that claim is
   * worth something only if the plugin is recent enough for its author to have meant it.
   *
   * Both attributes are read directly off the raw, not-yet-fully-parsed [document] - cheap, and it
   * avoids a chicken-and-egg problem: we don't yet know which of the two "full" parses to run, so we
   * can't get them from either of their outputs yet. `document.rootElement` is always available here -
   * it's the same JDOM `Document` [PluginBeanToIdePluginConverter] later reads unconditionally via
   * `document.rootElement` once one of the two parsers has run.
   *
   * Notes on the comparisons:
   *
   * - Comparing baselines is sufficient. `IdeVersionImpl.fromString` reduces `262.*` to baseline 262
   *   with snapshot components, strips a product code (`IU-262.*` -> 262), and treats a bare `262` as a
   *   baseline rather than a build number. No full-version comparison is needed.
   * - An unparseable `until-build` falls through to the `since-build` clause deliberately: a malformed
   *   upper bound is not a declaration of compatibility. Selection runs before validation and must not
   *   depend on it - the descriptor problem for a malformed bound is registered separately, by whichever
   *   parser then runs.
   * - A nested descriptor inherits the enclosing plugin's choice. Content modules, V2 module
   *   descriptors and `<depends config-file="...">` descriptors carry no `<idea-version>` of their own,
   *   so evaluating the rule against them would silently split a single plugin across both parsers.
   *   See [usedPlatformParser].
   */
  internal fun shouldUsePlatformParser(document: Document): Boolean {
    parentPlugin?.let { return it.usedPlatformParser }

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
    // Declares nothing and has no parent to inherit from - which is every descriptor loaded as part of
    // an IDE rather than as a plugin under verification. Its compatibility is not its own to declare:
    // it ships with the IDE, so the IDE's version is the only meaningful answer, and using it is what
    // keeps a bundled plugin and its module descriptors on the same parser.
    return containingIdeVersion?.let { it.baselineVersion >= CONDITIONAL_INCLUDE_REMOVAL_BASELINE } ?: false
  }

  /**
   * Derives the resource root a classloader would use for this descriptor - i.e. what the platform
   * parser's already-joined `<xi:include>` paths (e.g. `"META-INF/extensions.xml"`) are relative to,
   * see [com.jetbrains.plugin.structure.intellij.xinclude.ResourceResolverXIncludeLoader].
   *
   * [documentPath] is absolutised first, and that is load-bearing rather than cosmetic: a descriptor
   * inside a JAR is addressed by a path obtained from `FileSystem.getPath("META-INF/plugin.xml")`,
   * which is *relative* within the ZIP filesystem, so its `parent.parent` is `null` and nothing could
   * be derived at all - for nearly every plugin, since nearly all of them ship as JARs. Absolutised, it
   * becomes `/META-INF/plugin.xml` and the two shapes below fall out naturally.
   *
   * The shapes are distinguished by whether the descriptor's own parent directory is named `META-INF`:
   *
   *  - Main `plugin.xml` and V1 optional-dependency config-file descriptors
   *    (`<depends optional="true" config-file="...">`) both live directly under `<root>/META-INF/` -
   *    confirmed against `JarPluginLoader` (`jar.getPluginDescriptor("META-INF/$descriptorPath")`),
   *    `PluginDirectoryLoader` (`pluginDirectory.resolve(META_INF).resolve(descriptorPath)`) and
   *    `OptionalDependencyResolver` (loaded the same way, no `../` in that convention). Root is
   *    `parent.parent`: the ZIP root `/`, or the exploded plugin directory.
   *  - File-based content modules ([PluginModuleResolver]'s `"../$moduleName.xml"` convention,
   *    [com.jetbrains.plugin.structure.intellij.plugin.module.FileBasedModuleDescriptorResolver]) always
   *    land at some JAR's or directory's own root, never under `META-INF`: the forced `META-INF/` prefix
   *    the two loaders above always add cancels out against the module reference's own leading `../`.
   *    That root is either the main plugin artifact itself or, when the module ships as its own
   *    `lib/modules/<name>.jar`, that module's own JAR. Either way `parent` IS the root.
   *
   * [ResourceRootSource.ContainingDescriptor] covers the third shape, an inline content module
   * (`<module name="...">CDATA</module>`), which has no filesystem path at all: `DescriptorResource`
   * synthesises [documentPath] from a URI fragment into a bare, parentless single-segment path naming
   * no real file, so absolutising it would silently root the plugin at the current working directory.
   * Such a module inherits the containing descriptor's already-resolved root, which is the artifact its
   * CDATA came from.
   */
  private fun resolveResourceRoot(documentPath: Path, resourceRootSource: ResourceRootSource): Path? {
    if (resourceRootSource == ResourceRootSource.ContainingDescriptor) {
      return parentPlugin?.resourceRoot
    }
    val parent = documentPath.toAbsolutePath().parent ?: return parentPlugin?.resourceRoot
    return if (parent.fileName?.toString() == IdePluginManager.META_INF) parent.parent else parent
  }

  /**
   * How a descriptor's resource root is obtained, see [resolveResourceRoot].
   */
  private enum class ResourceRootSource {
    /** Derived from the descriptor's own path within its artifact. */
    OwnDocumentPath,

    /** Inherited from the containing descriptor, for a descriptor that has no path of its own. */
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
   * unresolved document straight to the library, which resolves `<xi:include>` itself. It does so
   * through [com.jetbrains.plugin.structure.intellij.xinclude.ResourceResolverXIncludeLoader], driven
   * by this very [pathResolver] anchored at [resourceRoot], so both parsers see the same resources -
   * including includes whose target lives in a sibling JAR of the plugin's `lib` directory. What
   * differs is only the two features the library has dropped: `includeIf`/`includeUnless`
   * (see [com.jetbrains.plugin.structure.intellij.problems.ConditionalIncludeNotSupported]) and
   * non-default `xpointer` values. `documentName` is consequently unused on this path - it was only
   * needed to name the document in [XIncluder]'s own error messages - kept as a parameter for symmetry
   * with [resolveDocumentAndValidateBeanViaJaxb] and because [resolveDocumentAndValidateBean] calls
   * both branches uniformly.
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
    val raw = platformDescriptorParser.parse(
      originalDocument, resourceRoot, pathResolver, descriptorPath, pluginFileName, validationContext
    )
    if (raw == null) {
      validationContext.problems.forEach { registerProblem(it) }
      return false
    }

    platformDescriptorValidator.validate(raw, originalDocument, validationContext, validateDescriptor)
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
