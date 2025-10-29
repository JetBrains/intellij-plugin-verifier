/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.ide.diff.builder.cli

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.extension
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.writeText
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginContentDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.output.teamcity.TeamCityHistory
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.output.teamcity.TeamCityTest
import com.jetbrains.pluginverifier.results.presentation.JvmDescriptorsPresentation
import com.jetbrains.pluginverifier.results.presentation.toSimpleJavaClassName
import com.jetbrains.pluginverifier.usages.deprecated.deprecationInfo
import com.jetbrains.pluginverifier.usages.experimental.resolveExperimentalApiAnnotation
import com.jetbrains.pluginverifier.usages.util.MemberAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassOrNull
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import org.jetbrains.ide.diff.builder.api.*
import org.jetbrains.ide.diff.builder.filter.ClassFilter
import org.jetbrains.ide.diff.builder.ide.buildIdeResources
import org.jetbrains.ide.diff.builder.ide.toSignature
import org.jetbrains.ide.diff.builder.persistence.externalAnnotations.externalPresentation
import org.jetbrains.ide.diff.builder.persistence.externalAnnotations.javaPackageName
import org.jetbrains.ide.diff.builder.persistence.json.JsonApiReportReader
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors
import kotlin.system.exitProcess

/**
 * [help]
 */
class ApiQualityCheckCommand : Command {

  companion object {
    private val LOG = LoggerFactory.getLogger("api-quality-check")
  }

  override val commandName
    get() = "api-quality-check"

  override val help
    get() = """
      1) Detect APIs marked experimental for too long.
      2) Detect APIs that should already be removed.
      
      api-quality-check <IDE> <metadata.json> -current-branch 193
    """.trimIndent()

  override fun execute(freeArgs: List<String>) {
    val cliOptions = CliOptions()
    val args = Args.parse(cliOptions, freeArgs.toTypedArray(), false)
    if (args.size < 2) {
      System.err.println("Paths to <IDE> <metadata.json> must be specified.")
      exitProcess(1)
    }

    val idePath = Paths.get(args[0])
    require(idePath.exists()) { "IDE does not exist: $idePath" }
    require(idePath.isDirectory) { "IDE is not a directory: $idePath" }

    val metadataPath = Paths.get(args[1])
    require(metadataPath.exists()) { "Metadata file does not exist: $metadataPath" }
    require(metadataPath.extension == "json") { "Metadata is not a .json file: $metadataPath" }

    val pluginsBuiltFromSources = readPluginsBuiltFromSources(cliOptions)

    val classFilter = cliOptions.classFilter()
    LOG.info(classFilter.toString())

    val currentBranch = cliOptions.currentBranch.toInt()
    val maxRemovalBranch = cliOptions.maxRemovalBranch.toInt()
    val minExperimentalBranch = cliOptions.minExperimentalBranch.toInt()
    val checkSfrVersionPresence = cliOptions.checkSfrVersionPresence.toBoolean()
    val qualityOptions = ApiQualityOptions(currentBranch, maxRemovalBranch, minExperimentalBranch, checkSfrVersionPresence)

    val metadata = JsonApiReportReader().readApiReport(metadataPath)

    val ide = IdeManager.createManager().createIde(idePath)
    val qualityReport = ApiQualityReport(ide.version, qualityOptions)
    checkApi(ide, classFilter, metadata, qualityOptions, qualityReport)
    findNonDynamicExtensionPoints(ide, pluginsBuiltFromSources, qualityReport)

    val tc = TeamCityLog(System.out)
    val newTcHistory = printReport(qualityReport, tc)
    newTcHistory.writeToFile(Paths.get("tc-tests.json"))

    val previousTcHistory = cliOptions.previousTcTestsFile?.let { Paths.get(it) }?.let { TeamCityHistory.readFromFile(it) }
    if (previousTcHistory != null) {
      newTcHistory.reportOldSkippedTestsSuccessful(previousTcHistory, tc)
    }
  }

  private fun readPluginsBuiltFromSources(cliOptions: CliOptions): List<IdePlugin> {
    val plugins = mutableListOf<IdePlugin>()
    val pluginsPath = cliOptions.pluginsBuiltFromSourcesPath?.let { Paths.get(it) }
    if (pluginsPath != null) {
      val pluginFiles = Files.list(pluginsPath).use { stream ->
        stream
          .filter { it.isDirectory || it.extension == "zip" || it.extension == "jar" }
          .collect(Collectors.toList())
      }
      for (pluginFile in pluginFiles) {
        LOG.info("Reading plugin frmo: $pluginFile")
        val idePlugin = with(IdePluginManager.createManager().createPlugin(pluginFile.toFile().toPath())) {
          when (this) {
            is PluginCreationSuccess -> plugin
            is PluginCreationFail -> null
          }
        } ?: continue
        plugins += idePlugin
      }
    }
    return plugins
  }

  private fun findNonDynamicExtensionPoints(
    ide: Ide,
    pluginsBuiltFromSources: List<IdePlugin>,
    qualityReport: ApiQualityReport
  ) {
    val nonDynamicExtensionPoints = hashSetOf<IdePluginContentDescriptor.ExtensionPoint>()
    val extensionPointsUsages = hashMapOf<String, Int>()

    val allPlugins = (ide.bundledPlugins + pluginsBuiltFromSources).filterIsInstance<IdePluginImpl>()
    for (idePlugin in allPlugins) {
      traverseOptionalPlugins(idePlugin) { plugin ->
        if (plugin !is IdePluginImpl) return@traverseOptionalPlugins

        sequenceOf(
          plugin.appContainerDescriptor,
          plugin.projectContainerDescriptor,
          plugin.moduleContainerDescriptor
        ).asSequence()
          .flatMap { it.extensionPoints.asSequence() }
          .filterNotTo(nonDynamicExtensionPoints) { it.isDynamic }

        for (epName in plugin.extensions.keys) {
          extensionPointsUsages.compute(epName) { _, count -> (count ?: 0) + 1 }
        }
      }
    }

    qualityReport.nonDynamicExtensionPoints += nonDynamicExtensionPoints.map { (extensionPointName) ->
      val numberOfUsages = extensionPointsUsages.getOrDefault(extensionPointName, 0)
      NonDynamicExtensionPoint(extensionPointName, numberOfUsages)
    }
  }

  private fun traverseOptionalPlugins(
    idePlugin: IdePlugin,
    visitedDescriptors: MutableSet<IdePlugin> = hashSetOf(),
    processor: (IdePlugin) -> Unit
  ) {
    processor(idePlugin)
    visitedDescriptors += idePlugin
    for (optionalDescriptor in idePlugin.optionalDescriptors) {
      val optionalPlugin = optionalDescriptor.optionalPlugin
      processor(optionalPlugin)
      if (idePlugin !in visitedDescriptors) {
        traverseOptionalPlugins(optionalPlugin, visitedDescriptors, processor)
      }
    }
  }

  private fun checkApi(
    ide: Ide,
    classFilter: ClassFilter,
    apiMetadata: ApiReport,
    qualityOptions: ApiQualityOptions,
    report: ApiQualityReport
  ) {
    buildIdeResources(ide, Resolver.ReadMode.SIGNATURES).use { ideResources ->
      val ideResolver = ideResources.allResolver
      for (className in ideResolver.allClasses) {
        if (classFilter.shouldProcessClass(className)) {
          val classFile = ideResolver.resolveClassOrNull(className) ?: continue
          checkApi(classFile, apiMetadata, ideResolver, qualityOptions, report)

          for (method in classFile.methods) {
            checkApi(method, apiMetadata, ideResolver, qualityOptions, report)
          }

          for (field in classFile.fields) {
            checkApi(field, apiMetadata, ideResolver, qualityOptions, report)
          }
        }
      }
    }
  }

  private fun printReport(report: ApiQualityReport, tc: TeamCityLog): TeamCityHistory {
    val failedTests = arrayListOf<TeamCityTest>()
    if (report.tooLongExperimental.isNotEmpty()) {
      for ((sinceVersion, tooLongExperimentalApis) in report.tooLongExperimental.groupBy { it.sinceVersion }) {
        val suiteName = "(API marked experimental since $sinceVersion)"
        tc.testSuiteStarted(suiteName).use {
          val (viaPackage, notViaPackage) = tooLongExperimentalApis.partition { it.experimentalMemberAnnotation is MemberAnnotation.AnnotatedViaPackage }
          for ((packageName, samePackage) in viaPackage.groupBy { (it.experimentalMemberAnnotation as MemberAnnotation.AnnotatedViaPackage).packageName }) {
            val javaPackageName = packageName.replace('/', '.')
            val testName = "($javaPackageName)"
            tc.testStarted(testName).use {
              val message = buildString {
                appendLine("The following APIs belonging to package '$javaPackageName' are marked with @ApiStatus.Experimental for too long")
                for ((apiSignature, _) in samePackage.sortedBy { it.apiSignature.fullPresentation }) {
                  append("  ").append(apiSignature.fullPresentation).append(" is marked experimental since $sinceVersion")
                  appendLine()
                }
                appendLine("The current branch is ${report.apiQualityOptions.currentBranch}")
                appendLine()
                appendLine(getExperimentalNote())
              }
              failedTests += TeamCityTest(suiteName, testName)
              tc.testFailed(testName, message, "")
            }
          }

          for ((signature, _, experimentalMemberAnnotation) in notViaPackage) {
            val testName = "(${signature.shortPresentation})"
            tc.testStarted(testName).use {
              val message = buildString {
                append(signature.fullPresentation)
                append(" is marked @ApiStatus.Experimental")
                append(
                  when (experimentalMemberAnnotation) {
                    is MemberAnnotation.AnnotatedDirectly -> ""
                    is MemberAnnotation.AnnotatedViaContainingClass -> " via containing class ${experimentalMemberAnnotation.containingClass.toSignature().fullPresentation}"
                    is MemberAnnotation.AnnotatedViaPackage -> " via containing package ${experimentalMemberAnnotation.packageName.replace('/', '.')}"
                  }
                )
                append(" since $sinceVersion, but the current branch is ${report.apiQualityOptions.currentBranch}. ")
                append(getExperimentalNote())
              }
              failedTests += TeamCityTest(suiteName, testName)
              tc.testFailed(testName, message, "")
            }
          }
        }
      }
    }

    if (report.mustAlreadyBeRemoved.isNotEmpty()) {
      for ((removalVersion, mustBeRemovedApis) in report.mustAlreadyBeRemoved.groupBy { it.removalVersion }) {
        val suiteName = "(API to be removed in $removalVersion)"
        tc.testSuiteStarted(suiteName).use {
          for ((signature, deprecatedInVersion, scheduledForRemovalInVersion, _) in mustBeRemovedApis) {
            val testName = "(${signature.shortPresentation})"
            tc.testStarted(testName).use {
              val message = buildString {
                append(signature.fullPresentation)
                if (removalVersion.branch < report.apiQualityOptions.currentBranch) {
                  append(" must have been ")
                } else {
                  append(" must be ")
                }
                append("removed in ${removalVersion.originalVersion}")
                appendLine()
                append("It was deprecated")
                if (deprecatedInVersion != null) {
                  append(" in $deprecatedInVersion")
                } else {
                  append(" before ${BuildIdeApiAnnotationsCommand.MIN_BUILD_NUMBER.baselineVersion}")
                }
                if (scheduledForRemovalInVersion != null && scheduledForRemovalInVersion.baselineVersion <= removalVersion.branch) {
                  append(" and marked with @ApiStatus.ScheduledForRemoval annotation in $scheduledForRemovalInVersion to be removed in $removalVersion")
                }
                appendLine()
                append("Consider removing this API right now or promoting planned removal version a little bit if there are too many plugins still using it.")
              }
              failedTests += TeamCityTest(suiteName, testName)
              tc.testFailed(testName, message, "")
            }
          }
        }
      }
    }

    if (report.sfrApisWithWrongPlannedVersion.isNotEmpty()) {
      val suiteName = "(Planned removal version of API must be specified in YYYY.R format)"
      tc.testSuiteStarted(suiteName).use {
        for ((signature, deprecatedInVersion, scheduledForRemovalInVersion, inVersionValue) in report.sfrApisWithWrongPlannedVersion) {
          val testName = "(${signature.shortPresentation})"
          tc.testStarted(testName).use {
            val message = buildString {
              append(signature.fullPresentation)
              append(" is scheduled to be removed. Its 'inVersion' value must be in YYYY.R format, like 2020.3, but ")
              if (inVersionValue != null) {
                append("it is '$inVersionValue'")
              } else {
                append("it is not specified")
              }
              appendLine()
              append("API was deprecated")
              if (deprecatedInVersion != null) {
                append(" in $deprecatedInVersion")
              } else {
                append(" before ${BuildIdeApiAnnotationsCommand.MIN_BUILD_NUMBER.baselineVersion}")
              }
              if (scheduledForRemovalInVersion != null) {
                append(" and scheduled for removal in $scheduledForRemovalInVersion.")
              }
            }
            tc.testFailed(testName, message, "")
          }
        }
      }
    }

    val stabilizedApis = report.stabilizedExperimentalApis
    if (stabilizedApis.isNotEmpty()) {
      val stabilizedMessage = buildString {
        appendLine("The following APIs have become stable (not marked with @ApiStatus.Experimental) in branch ${report.apiQualityOptions.currentBranch}")
        appendLine("These APIs may be advertised on https://plugins.jetbrains.com/docs/intellij/api-notable.html")
        appendLine()
        for ((_, apisOfPackage) in stabilizedApis.sortedBy { it.apiSignature.javaPackageName }.groupBy { it.apiSignature.javaPackageName }) {
          for ((signature, inVersion) in apisOfPackage) {
            appendLine("${signature.externalPresentation} was unmarked @ApiStatus.Experimental in $inVersion")
          }
          appendLine("")
        }
      }
      Paths.get("stabilized-experimental-apis.txt").writeText(stabilizedMessage)
    }

    if (report.tooLongExperimental.isEmpty()
      && report.mustAlreadyBeRemoved.isEmpty()
      && report.sfrApisWithWrongPlannedVersion.isEmpty()
    ) {
      tc.buildStatusSuccess("API of ${report.ideVersion} is OK")
    } else {
      val buildMessage = buildString {
        append("In ${report.ideVersion} found ")
        if (report.tooLongExperimental.isNotEmpty()) {
          append("${report.tooLongExperimental.size} stale experimental APIs")
        }
        if (report.mustAlreadyBeRemoved.isNotEmpty()) {
          if (report.tooLongExperimental.isNotEmpty()) {
            append(" and ")
          }
          append("${report.mustAlreadyBeRemoved.size} APIs to be removed")
        }
        if (report.sfrApisWithWrongPlannedVersion.isNotEmpty()) {
          if (report.tooLongExperimental.isNotEmpty() || report.mustAlreadyBeRemoved.isNotEmpty()) {
            append(" and ")
          }
          append("${report.sfrApisWithWrongPlannedVersion.size} incorrect planned removal API versions.")
        }
      }
      tc.buildStatusFailure(buildMessage)
    }

    val nonDynamicExtensionPoints = report.nonDynamicExtensionPoints
    if (nonDynamicExtensionPoints.isNotEmpty()) {
      Paths.get("non-dynamic-extension-points.csv").writeText(
        buildString {
          appendLine("EP name,Usages")
          nonDynamicExtensionPoints.sortedByDescending { it.numberOfUsages }.forEach { (extensionPointName, numberOfUsages) ->
            appendLine("$extensionPointName,$numberOfUsages")
          }
        }
      )
    }

    return TeamCityHistory(failedTests)
  }

  private fun getExperimentalNote(): String = buildString {
    append("API shouldn't be marked @Experimental for too long. ")
    append("Please consider clarifying the API status by removing @Experimental and making it usable by external developers without hesitation. ")
    append("Also verify that Javadoc is up to date ")
    append("and consider advertising this API on https://plugins.jetbrains.com/docs/intellij/api-notable.html")
  }

  private val ApiSignature.shortPresentation: String
    get() = when (this) {
      is ClassSignature -> toSimpleJavaClassName(className)
      is MethodSignature -> buildString {
        append(hostSignature.shortPresentation + ".")
        if (methodName == "<init>") {
          append(hostSignature.className.substringAfterLast('/').substringAfterLast('$'))
        } else {
          append(methodName)
        }
        val (rawParamTypes, _) = JvmDescriptorsPresentation.splitMethodDescriptorOnRawParametersAndReturnTypes(methodDescriptor)
        append("(")
        append(rawParamTypes.joinToString { JvmDescriptorsPresentation.convertJvmDescriptorToNormalPresentation(it, toSimpleJavaClassName) })
        append(")")
      }
      is FieldSignature -> hostSignature.shortPresentation + "." + fieldName
    }

  private val ApiSignature.fullPresentation
    get() = externalPresentation

  private fun checkApi(
    classFileMember: ClassFileMember,
    apiMetadata: ApiReport,
    ideResolver: Resolver,
    qualityOptions: ApiQualityOptions,
    qualityReport: ApiQualityReport
  ) {
    val signature = classFileMember.toSignature()
    val apiEvents = apiMetadata[signature]

    val experimentalMemberAnnotation = classFileMember.resolveExperimentalApiAnnotation(ideResolver)
    if (experimentalMemberAnnotation != null) {
      if (experimentalMemberAnnotation !is MemberAnnotation.AnnotatedViaContainingClass) {
        val since = apiEvents.filterIsInstance<MarkedExperimentalIn>().map { it.ideVersion }.minOrNull()
        if (since != null && since.baselineVersion <= qualityOptions.minExperimentalBranch) {
          qualityReport.tooLongExperimental += TooLongExperimental(signature, since, experimentalMemberAnnotation)
        }
      }
    } else {
      val unmarkedExperimentalIn = apiEvents.filterIsInstance<UnmarkedExperimentalIn>().map { it.ideVersion }.maxOrNull()
      if (unmarkedExperimentalIn != null && unmarkedExperimentalIn.baselineVersion >= qualityOptions.currentBranch) {
        qualityReport.stabilizedExperimentalApis += StabilizedExperimentalApi(signature, unmarkedExperimentalIn)
      }
    }

    val deprecationInfo = classFileMember.deprecationInfo
    if (deprecationInfo != null && deprecationInfo.forRemoval) {
      val markedDeprecated = apiEvents.filterIsInstance<MarkedDeprecatedIn>()
      val unmarkedDeprecated = apiEvents.filterIsInstance<UnmarkedDeprecatedIn>()

      val firstDeprecated = markedDeprecated.map { it.ideVersion }.minOrNull()
      val firstUnDeprecated = unmarkedDeprecated.map { it.ideVersion }.minOrNull()

      val scheduledForRemovalInVersion = markedDeprecated.filter { it.forRemoval }.minByOrNull { it.ideVersion }?.ideVersion

      val wasDeprecatedBeforeFirstKnownIde = firstDeprecated != null && firstUnDeprecated != null && firstUnDeprecated <= firstDeprecated
      val deprecatedInVersion = if (wasDeprecatedBeforeFirstKnownIde) {
        null
      } else {
        markedDeprecated.minByOrNull { it.ideVersion }?.ideVersion
      }

      val untilVersionStr = deprecationInfo.untilVersion
      val removalVersion = untilVersionStr?.let { RemovalVersion.parseRemovalVersion(it) }
      if (removalVersion != null) {
        if (removalVersion.branch <= qualityOptions.maxRemovalBranch) {
          qualityReport.mustAlreadyBeRemoved += MustAlreadyBeRemoved(
            signature,
            deprecatedInVersion,
            scheduledForRemovalInVersion,
            removalVersion
          )
        }
      } else if (untilVersionStr != null || qualityOptions.checkSfrVersionPresence) {
        qualityReport.sfrApisWithWrongPlannedVersion += SfrApiWithWrongPlannedVersion(
          signature,
          deprecatedInVersion,
          scheduledForRemovalInVersion,
          untilVersionStr
        )
      }
    }
  }

  class CliOptions : IdeDiffCommand.CliOptions() {
    @set:Argument("current-branch", description = "Current release IDE branch")
    var currentBranch: String = "201"

    @set:Argument(
      "max-removal-branch", description = "Branch number used to find APIs that must already be removed. " +
      "All @ScheduledForRemoval APIs will be found where 'inVersion' <= 'max-removal-branch'."
    )
    var maxRemovalBranch: String = "201"

    @set:Argument(
      "min-experimental-branch", description = "Branch number used to find APIs that are marked experimental foo too long. " +
      "All @Experimental APIs will be found where 'API introduction version' <= 'min-experimental-branch'."
    )
    var minExperimentalBranch: String = "191"

    @set:Argument("previous-tc-tests-file", description = "File containing TeamCity tests that were run in the previous build. ")
    var previousTcTestsFile: String? = null

    @set:Argument("sfr-check-version-presence", description = "Whether @ApiStatus.ScheduledForRemoval APIs must have 'inVersion' value specified ('true' by default)")
    var checkSfrVersionPresence: String = "true"

    @set:Argument("plugins-built-from-sources", description = "Directory containing plugins built from the same sources as IDE. ")
    var pluginsBuiltFromSourcesPath: String? = null
  }

}

private data class ApiQualityOptions(
  val currentBranch: Int,
  val maxRemovalBranch: Int,
  val minExperimentalBranch: Int,
  val checkSfrVersionPresence: Boolean
)

private data class ApiQualityReport(
  val ideVersion: IdeVersion,
  val apiQualityOptions: ApiQualityOptions,
  val tooLongExperimental: MutableList<TooLongExperimental> = arrayListOf(),
  val mustAlreadyBeRemoved: MutableList<MustAlreadyBeRemoved> = arrayListOf(),
  val stabilizedExperimentalApis: MutableList<StabilizedExperimentalApi> = arrayListOf(),
  val sfrApisWithWrongPlannedVersion: MutableList<SfrApiWithWrongPlannedVersion> = arrayListOf(),
  val nonDynamicExtensionPoints: MutableList<NonDynamicExtensionPoint> = arrayListOf()
)

private data class MustAlreadyBeRemoved(
  val apiSignature: ApiSignature,
  val deprecatedInVersion: IdeVersion?,
  val scheduledForRemovalInVersion: IdeVersion?,
  val removalVersion: RemovalVersion
)

private data class SfrApiWithWrongPlannedVersion(
  val apiSignature: ApiSignature,
  val deprecatedInVersion: IdeVersion?,
  val scheduledForRemovalInVersion: IdeVersion?,
  val inVersionValue: String?
)

private data class RemovalVersion(val originalVersion: String, val branch: Int) {

  companion object {
    private val IDE_RELEASE_REGEX = Regex("(\\d\\d\\d\\d)\\.(\\d)")

    fun parseRemovalVersion(version: String): RemovalVersion? {
      val match = IDE_RELEASE_REGEX.matchEntire(version) ?: return null
      val year = match.groupValues[1].toInt()
      val release = match.groupValues[2].toInt()
      val branch = (year - 2000) * 10 + release
      return RemovalVersion(version, branch)
    }
  }

  override fun toString() = originalVersion
}

private data class TooLongExperimental(
  val apiSignature: ApiSignature,
  val sinceVersion: IdeVersion,
  val experimentalMemberAnnotation: MemberAnnotation
)

private data class StabilizedExperimentalApi(
  val apiSignature: ApiSignature,
  val unmarkedExperimentalIn: IdeVersion
)

private data class NonDynamicExtensionPoint(
  val extensionPointName: String,
  val numberOfUsages: Int
)
