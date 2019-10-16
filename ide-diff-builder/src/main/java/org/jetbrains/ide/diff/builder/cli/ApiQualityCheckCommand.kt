package org.jetbrains.ide.diff.builder.cli

import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.results.presentation.JvmDescriptorsPresentation
import com.jetbrains.pluginverifier.results.presentation.toSimpleJavaClassName
import com.jetbrains.pluginverifier.usages.deprecated.deprecationInfo
import com.jetbrains.pluginverifier.usages.experimental.findEffectiveExperimentalAnnotation
import com.jetbrains.pluginverifier.usages.util.MemberAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassOrNull
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import kotlinx.serialization.Serializable
import org.jetbrains.ide.diff.builder.api.*
import org.jetbrains.ide.diff.builder.ide.buildIdeResources
import org.jetbrains.ide.diff.builder.ide.toSignature
import org.jetbrains.ide.diff.builder.persistence.externalAnnotations.externalPresentation
import org.jetbrains.ide.diff.builder.persistence.json.JsonApiReportReader
import org.jetbrains.ide.diff.builder.persistence.json.jsonInstance
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths
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
      1) Detect APIs marked experimental for more than N (default: 3) release branches.
      2) Detect APIs marked to be removed in a branch smaller than the current one
      
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

    val classFilter = cliOptions.classFilter()
    LOG.info(classFilter.toString())

    val currentBranch = cliOptions.currentBranch.toInt()
    val maxRemovalBranch = cliOptions.maxRemovalBranch.toInt()
    val maxExperimentalBranches = cliOptions.maxExperimentalBranches.toInt()
    val qualityOptions = ApiQualityOptions(currentBranch, maxRemovalBranch, maxExperimentalBranches)

    val metadata = JsonApiReportReader().readApiReport(metadataPath)

    val ide = IdeManager.createManager().createIde(idePath.toFile())
    val report = ApiQualityReport(ide.version, qualityOptions)
    buildIdeResources(ide, Resolver.ReadMode.SIGNATURES).use { ideResources ->
      val ideResolver = ideResources.allResolver
      for (className in ideResolver.allClasses) {
        if (classFilter.shouldProcessClass(className)) {
          val classFile = ideResolver.resolveClassOrNull(className) ?: continue
          checkApi(classFile, metadata, ideResolver, qualityOptions, report)

          for (method in classFile.methods) {
            checkApi(method, metadata, ideResolver, qualityOptions, report)
          }

          for (field in classFile.fields) {
            checkApi(field, metadata, ideResolver, qualityOptions, report)
          }
        }
      }
    }

    val tc = TeamCityLog(System.out)
    val newTcHistory = printReport(report, tc)
    newTcHistory.writeToFile(Paths.get("tc-tests.json"))

    val previousTcHistory = cliOptions.previousTcTestsFile?.let { Paths.get(it) }?.let { ApiQualityTeamCityHistory.readFromFile(it) }
    if (previousTcHistory != null) {
      reportOldSkippedTestsSuccessful(previousTcHistory, newTcHistory, tc)
    }
  }

  private fun reportOldSkippedTestsSuccessful(
      previousTests: ApiQualityTeamCityHistory,
      newTests: ApiQualityTeamCityHistory,
      tc: TeamCityLog
  ) {
    val skippedTests = previousTests.tests - newTests.tests
    for ((suiteName, tests) in skippedTests.groupBy { it.suiteName }) {
      tc.testSuiteStarted(suiteName).use {
        for (test in tests) {
          tc.testStarted(test.testName).close()
        }
      }
    }
  }

  @Serializable
  private data class ApiQualityTeamCityTest(val suiteName: String, val testName: String)

  @Serializable
  private data class ApiQualityTeamCityHistory(val tests: List<ApiQualityTeamCityTest>) {
    companion object {
      fun readFromFile(file: Path): ApiQualityTeamCityHistory =
          jsonInstance.parse(serializer(), file.readText())
    }

    fun writeToFile(file: Path) {
      file.writeText(jsonInstance.stringify(serializer(), this))
    }
  }

  private fun printReport(report: ApiQualityReport, tc: TeamCityLog): ApiQualityTeamCityHistory {
    val failedTests = arrayListOf<ApiQualityTeamCityTest>()
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
                appendln("The following APIs belonging to package '$javaPackageName' are marked with @ApiStatus.Experimental for more than ${report.apiQualityOptions.maxExperimentalBranches} branches")
                for ((apiSignature, _) in samePackage.sortedBy { it.apiSignature.fullPresentation }) {
                  append("  ").append(apiSignature.fullPresentation).append(" is marked experimental since $sinceVersion")
                  appendln()
                }
                appendln("The current branch is ${report.apiQualityOptions.currentBranch}")
                appendln()
                appendln(getExperimentalNote(report))
              }
              failedTests += ApiQualityTeamCityTest(suiteName, testName)
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
                append(getExperimentalNote(report))
              }
              failedTests += ApiQualityTeamCityTest(suiteName, testName)
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
                appendln()
                append("It was deprecated")
                if (deprecatedInVersion != null) {
                  append(" in $deprecatedInVersion")
                } else {
                  append(" before ${BuildIdeApiAnnotationsCommand.MIN_BUILD_NUMBER.baselineVersion}")
                }
                if (scheduledForRemovalInVersion != null && scheduledForRemovalInVersion.baselineVersion <= removalVersion.branch) {
                  append(" and scheduled for removal in $scheduledForRemovalInVersion.")
                }
                appendln()
                append("Consider removing this API right now or promoting planned removal version a little bit if there are too many plugins still using it.")
              }
              failedTests += ApiQualityTeamCityTest(suiteName, testName)
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
              appendln()
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

    if (report.stabilizedExperimentalApis.isNotEmpty()) {
      val stabilizedMessage = buildString {
        appendln("The following APIs have become stable (not marked with @ApiStatus.Experimental) in branch ${report.apiQualityOptions.currentBranch}")
        appendln("These APIs may be advertised on http://www.jetbrains.org/intellij/sdk/docs/reference_guide/api_notable/api_notable_list_2019.html")
        appendln()
        for ((signature, inVersion) in report.stabilizedExperimentalApis) {
          appendln("${signature.externalPresentation} was unmarked @ApiStatus.Experimental in $inVersion")
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

    return ApiQualityTeamCityHistory(failedTests)
  }

  private fun getExperimentalNote(report: ApiQualityReport): String = buildString {
    append("API shouldn't be marked @Experimental for too long (more than ${report.apiQualityOptions.maxExperimentalBranches} releases). ")
    append("Please consider clarifying the API status by removing @Experimental and making it usable by external developers without hesitation. ")
    append("Also verify that Javadoc is up to date ")
    append("and consider advertising this API on https://www.jetbrains.org/intellij/sdk/docs/reference_guide/api_notable/api_notable.html.")
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

    val experimentalMemberAnnotation = classFileMember.findEffectiveExperimentalAnnotation(ideResolver)
    if (experimentalMemberAnnotation != null) {
      if (experimentalMemberAnnotation !is MemberAnnotation.AnnotatedViaContainingClass) {
        val since = apiEvents.filterIsInstance<MarkedExperimentalIn>().map { it.ideVersion }.min()
        if (since != null && since.baselineVersion + qualityOptions.maxExperimentalBranches < qualityOptions.currentBranch) {
          qualityReport.tooLongExperimental += TooLongExperimental(signature, since, experimentalMemberAnnotation)
        }
      }
    } else {
      val unmarkedExperimentalIn = apiEvents.filterIsInstance<UnmarkedExperimentalIn>().map { it.ideVersion }.max()
      if (unmarkedExperimentalIn != null && unmarkedExperimentalIn.baselineVersion >= qualityOptions.currentBranch) {
        qualityReport.stabilizedExperimentalApis += StabilizedExperimentalApi(signature, unmarkedExperimentalIn)
      }
    }

    val deprecationInfo = classFileMember.deprecationInfo
    if (deprecationInfo != null && deprecationInfo.forRemoval) {
      val markedDeprecated = apiEvents.filterIsInstance<MarkedDeprecatedIn>()
      val unmarkedDeprecated = apiEvents.filterIsInstance<UnmarkedDeprecatedIn>()

      val firstDeprecated = markedDeprecated.map { it.ideVersion }.min()
      val firstUnDeprecated = unmarkedDeprecated.map { it.ideVersion }.min()

      val scheduledForRemovalInVersion = markedDeprecated.filter { it.forRemoval }.minBy { it.ideVersion }?.ideVersion

      val wasDeprecatedBeforeFirstKnownIde = firstDeprecated != null && firstUnDeprecated != null && firstUnDeprecated <= firstDeprecated
      val deprecatedInVersion = if (wasDeprecatedBeforeFirstKnownIde) {
        null
      } else {
        markedDeprecated.minBy { it.ideVersion }?.ideVersion
      }

      val removalVersion = deprecationInfo.untilVersion?.let { RemovalVersion.parseRemovalVersion(it) }
      if (removalVersion != null) {
        if (removalVersion.branch <= qualityOptions.maxRemovalBranch) {
          qualityReport.mustAlreadyBeRemoved += MustAlreadyBeRemoved(
              signature,
              deprecatedInVersion,
              scheduledForRemovalInVersion,
              removalVersion
          )
        }
      } else {
        qualityReport.sfrApisWithWrongPlannedVersion += SfrApiWithWrongPlannedVersion(
            signature,
            deprecatedInVersion,
            scheduledForRemovalInVersion,
            deprecationInfo.untilVersion
        )
      }
    }
  }

  class CliOptions : IdeDiffCommand.CliOptions() {
    @set:Argument("current-branch", description = "Current release IDE branch")
    var currentBranch: String = "193"

    @set:Argument(
        "max-removal-branch", description = "Branch number used to find APIs that must already be removed. " +
        "All @ScheduledForRemoval APIs will be found where 'inVersion' <= 'max-removal-branch'."
    )
    var maxRemovalBranch: String = "193"

    @set:Argument("max-experimental-branches", description = "Maximum number of branches in which an API may stay experimental.")
    var maxExperimentalBranches: String = "3"

    @set:Argument("previous-tc-tests-file", description = "File containing TeamCity tests that were run in the previous build. ")
    var previousTcTestsFile: String? = null
  }

}

private data class ApiQualityOptions(
    val currentBranch: Int,
    val maxRemovalBranch: Int,
    val maxExperimentalBranches: Int
)

private data class ApiQualityReport(
    val ideVersion: IdeVersion,
    val apiQualityOptions: ApiQualityOptions,
    val tooLongExperimental: MutableList<TooLongExperimental> = arrayListOf(),
    val mustAlreadyBeRemoved: MutableList<MustAlreadyBeRemoved> = arrayListOf(),
    val stabilizedExperimentalApis: MutableList<StabilizedExperimentalApi> = arrayListOf(),
    val sfrApisWithWrongPlannedVersion: MutableList<SfrApiWithWrongPlannedVersion> = arrayListOf()
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
    private val IDE_RELEASE_REGEX = Regex("(\\d\\d\\d\\d)(\\.\\d)?")

    fun parseRemovalVersion(version: String): RemovalVersion? {
      val match = IDE_RELEASE_REGEX.matchEntire(version) ?: return null
      val year = match.groupValues[1].toInt()
      val release = match.groups[2]?.value?.drop(1)?.toInt()
      val branch = if (release != null) {
        (year - 2000) * 10 + release
      } else {
        (year - 2000) * 10 + 3
      }
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