package org.jetbrains.ide.diff.builder.cli

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.extension
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.writeText
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
import org.jetbrains.ide.diff.builder.api.*
import org.jetbrains.ide.diff.builder.ide.buildIdeResources
import org.jetbrains.ide.diff.builder.ide.toSignature
import org.jetbrains.ide.diff.builder.persistence.externalAnnotations.externalPresentation
import org.jetbrains.ide.diff.builder.persistence.json.JsonApiReportReader
import org.slf4j.LoggerFactory
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
    val maxExperimentalBranches = cliOptions.maxExperimentalBranches.toInt()
    val qualityOptions = ApiQualityOptions(currentBranch, maxExperimentalBranches)

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
    printReport(report)
  }

  private fun printReport(report: ApiQualityReport) {
    val tc = TeamCityLog(System.out)
    if (report.tooLongExperimental.isNotEmpty()) {
      for ((sinceVersion, tooLongExperimentalApis) in report.tooLongExperimental.groupBy { it.sinceVersion }) {
        tc.testSuiteStarted("(API marked experimental since $sinceVersion)").use {
          val (viaPackage, notViaPackage) = tooLongExperimentalApis.partition { it.experimentalMemberAnnotation is MemberAnnotation.AnnotatedViaPackage }
          for ((packageName, samePackage) in viaPackage.groupBy { (it.experimentalMemberAnnotation as MemberAnnotation.AnnotatedViaPackage).packageName }) {
            val javaPackageName = packageName.replace('/', '.')
            val testName = "($javaPackageName)"
            tc.testStarted(testName).use {
              val message = buildString {
                appendln("Package '$javaPackageName' is marked @ApiStatus.Experimental since $sinceVersion, but the current branch is ${report.apiQualityOptions.currentBranch}. ")
                appendln(getExperimentalNote(report))
                appendln()
                appendln("The following APIs belong to the package '$javaPackageName': " + samePackage.map { it.apiSignature.fullPresentation }.sorted().joinToString())
              }
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
              tc.testFailed(testName, message, "")
            }
          }
        }
      }
    }

    if (report.mustAlreadyBeRemoved.isNotEmpty()) {
      for ((removalVersion, mustBeRemovedApis) in report.mustAlreadyBeRemoved.groupBy { it.removalVersion }) {
        tc.testSuiteStarted("(API to be removed in $removalVersion)").use {
          for ((signature, deprecatedInVersion, scheduledForRemovalInVersion, _) in mustBeRemovedApis) {
            val testName = "(${signature.shortPresentation})"
            tc.testStarted(testName).use {
              val message = buildString {
                append(signature.fullPresentation)
                if (removalVersion.branch < report.apiQualityOptions.currentBranch) {
                  append("must have been ")
                } else {
                  append("must be ")
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
                append("If this API does not have external usages, consider removing it right now. ")
                append("Otherwise, reach out to the external developers and ask them to stop using it ASAP. ")
                append("Also consider promoting planned removal version a little bit.")
              }
              tc.testFailed(testName, message, "")
            }
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
      Paths.get("").resolve("stabilized-experimental-apis.txt").writeText(stabilizedMessage)
    }

    if (report.tooLongExperimental.isEmpty() && report.mustAlreadyBeRemoved.isEmpty()) {
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
      }
      tc.buildStatusFailure(buildMessage)
    }
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
      val since = apiEvents.filterIsInstance<MarkedExperimentalIn>().map { it.ideVersion }.min()
      if (since != null && since.baselineVersion + qualityOptions.maxExperimentalBranches < qualityOptions.currentBranch) {
        qualityReport.tooLongExperimental += TooLongExperimental(signature, since, experimentalMemberAnnotation)
      }
    } else {
      val unmarkedExperimentalIn = apiEvents.filterIsInstance<UnmarkedExperimentalIn>().map { it.ideVersion }.max()
      if (unmarkedExperimentalIn != null && unmarkedExperimentalIn.baselineVersion >= qualityOptions.currentBranch) {
        qualityReport.stabilizedExperimentalApis += StabilizedExperimentalApi(signature, unmarkedExperimentalIn)
      }
    }

    val deprecationInfo = classFileMember.deprecationInfo
    val removalVersion = deprecationInfo?.untilVersion?.let { RemovalVersion.parseRemovalVersion(it) }
    if (deprecationInfo != null
        && deprecationInfo.forRemoval
        && removalVersion != null
    ) {
      if (removalVersion.branch <= qualityOptions.currentBranch) {
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

        qualityReport.mustAlreadyBeRemoved += MustAlreadyBeRemoved(
            signature,
            deprecatedInVersion,
            scheduledForRemovalInVersion,
            removalVersion
        )
      }
    }
  }

  class CliOptions : IdeDiffCommand.CliOptions() {
    @set:Argument("current-branch", description = "Current release IDE branch. It is used to determine which APIs must already be removed")
    var currentBranch: String = "192"

    @set:Argument("max-experimental-branches", description = "Maximum number of branches in which an API may stay experimental.")
    var maxExperimentalBranches: String = "3"
  }

}

private data class ApiQualityOptions(
    val currentBranch: Int,
    val maxExperimentalBranches: Int
)

private data class ApiQualityReport(
    val ideVersion: IdeVersion,
    val apiQualityOptions: ApiQualityOptions,
    val tooLongExperimental: MutableList<TooLongExperimental> = arrayListOf(),
    val mustAlreadyBeRemoved: MutableList<MustAlreadyBeRemoved> = arrayListOf(),
    val stabilizedExperimentalApis: MutableList<StabilizedExperimentalApi> = arrayListOf()
)

private data class MustAlreadyBeRemoved(
    val apiSignature: ApiSignature,
    val deprecatedInVersion: IdeVersion?,
    val scheduledForRemovalInVersion: IdeVersion?,
    val removalVersion: RemovalVersion
)

private data class RemovalVersion(val originalVersion: String, val branch: Int) {

  companion object {
    private val BRANCH_REGEX = Regex("\\d\\d\\d")
    private val IDE_RELEASE_REGEX = Regex("(\\d\\d\\d\\d)(\\.\\d)?")

    fun parseRemovalVersion(version: String): RemovalVersion? =
        parseAsReleaseVersion(version) ?: parseAsBranch(version)

    private fun parseAsBranch(version: String): RemovalVersion? {
      val match = BRANCH_REGEX.matchEntire(version) ?: return null
      val branch = match.groupValues[1].toInt()
      return RemovalVersion(version, branch)
    }

    private fun parseAsReleaseVersion(version: String): RemovalVersion? {
      val match = IDE_RELEASE_REGEX.matchEntire(version) ?: return null
      val year = match.groupValues[1].toInt()
      val release = match.groups[2]?.value?.toInt()
      val branch = if (release != null) {
        (year - 2000) * 10 + release
      } else {
        (year - 2000) * 10 + 3
      }
      return RemovalVersion(version, branch)
    }
  }
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