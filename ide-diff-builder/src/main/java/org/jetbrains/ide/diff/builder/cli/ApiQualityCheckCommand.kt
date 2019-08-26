package org.jetbrains.ide.diff.builder.cli

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.extension
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.ide.VersionComparatorUtil
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.results.presentation.JvmDescriptorsPresentation
import com.jetbrains.pluginverifier.results.presentation.toSimpleJavaClassName
import com.jetbrains.pluginverifier.usages.deprecated.deprecationInfo
import com.jetbrains.pluginverifier.usages.experimental.isExperimentalApi
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
          for ((signature, _) in tooLongExperimentalApis) {
            val testName = "(${signature.shortPresentation})"
            tc.testStarted(testName).use {
              val message = """
              ${signature.fullPresentation} is experimental since $sinceVersion, while the current branch is ${report.apiQualityOptions.currentBranch}.
              Consider making this API public.
            """.trimIndent()
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
                appendln("${signature.fullPresentation} must have been removed in $removalVersion.")
                if (deprecatedInVersion != null) {
                  append("It was deprecated")
                  when {
                    deprecatedInVersion == scheduledForRemovalInVersion -> {
                      append(" and scheduled for removal in $deprecatedInVersion.")
                    }
                    scheduledForRemovalInVersion != null -> {
                      append(" in $deprecatedInVersion and scheduled for removal in $scheduledForRemovalInVersion")
                    }
                    else -> append(" in $deprecatedInVersion")
                  }
                  appendln()
                }
              }
              tc.testFailed(testName, message, "")
            }
          }
        }
      }
    }

    if (report.tooLongExperimental.isEmpty() && report.mustAlreadyBeRemoved.isEmpty()) {
      tc.buildStatusSuccess("API of ${report.ideVersion} is OK")
    } else {
      val buildMessage = buildString {
        append("Found ")
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

    if (classFileMember.isExperimentalApi(ideResolver)) {
      val since = apiMetadata[signature].filterIsInstance<MarkedExperimentalIn>().map { it.ideVersion }.min()
      if (since != null && since.baselineVersion + qualityOptions.maxExperimentalBranches < qualityOptions.currentBranch) {
        qualityReport.tooLongExperimental += TooLongExperimental(signature, since)
      }
    }

    val deprecationInfo = classFileMember.deprecationInfo
    val untilVersion = deprecationInfo?.untilVersion
    if (deprecationInfo != null
        && deprecationInfo.forRemoval
        && untilVersion != null
    ) {
      if (VersionComparatorUtil.compare(untilVersion, qualityOptions.currentBranch.toString()) < 0
          || VersionComparatorUtil.compare(untilVersion, branchToReleaseVersion(qualityOptions.currentBranch)) < 0) {

        val markedDeprecated = apiMetadata[signature].filterIsInstance<MarkedDeprecatedIn>()
        val deprecatedInVersion = markedDeprecated.minBy { it.ideVersion }?.ideVersion
        val scheduledForRemovalInVersion = markedDeprecated.filter { it.forRemoval }.minBy { it.ideVersion }?.ideVersion

        qualityReport.mustAlreadyBeRemoved += MustAlreadyBeRemoved(
            signature,
            deprecatedInVersion,
            scheduledForRemovalInVersion,
            untilVersion
        )
      }
    }
  }

  private fun branchToReleaseVersion(branch: Int): String =
      (2000 + branch / 10).toString() + "." + (branch % 10).toString()

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
    val mustAlreadyBeRemoved: MutableList<MustAlreadyBeRemoved> = arrayListOf()
)

private data class MustAlreadyBeRemoved(
    val apiSignature: ApiSignature,
    val deprecatedInVersion: IdeVersion?,
    val scheduledForRemovalInVersion: IdeVersion?,
    val removalVersion: String
)

private data class TooLongExperimental(
    val apiSignature: ApiSignature,
    val sinceVersion: IdeVersion
)