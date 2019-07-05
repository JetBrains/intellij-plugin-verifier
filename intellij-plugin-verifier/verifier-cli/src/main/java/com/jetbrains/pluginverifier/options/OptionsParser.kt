package com.jetbrains.pluginverifier.options

import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.ide.IdeResourceUtil
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import com.jetbrains.pluginverifier.parameters.filtering.*
import com.jetbrains.pluginverifier.parameters.packages.PackageFilter
import com.jetbrains.pluginverifier.repository.PluginIdAndVersion
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

object OptionsParser {

  private val LOG = LoggerFactory.getLogger(OptionsParser::class.java)

  private val TIMESTAMP_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd 'at' HH.mm.ss")

  private fun getVerificationReportsDirectory(opts: CmdOpts): Path {
    val reportDirectory = opts.verificationReportsDir?.let { Paths.get(it) }
    if (reportDirectory != null) {
      if (reportDirectory.exists() && reportDirectory.listFiles().isNotEmpty()) {
        LOG.info("Delete the verification directory ${reportDirectory.toAbsolutePath()} because it isn't empty")
        reportDirectory.deleteLogged()
      }
      reportDirectory.createDir()
      return reportDirectory
    }
    val nowTime = TIMESTAMP_DATE_FORMAT.format(Date())
    val directoryName = ("verification-$nowTime").replaceInvalidFileNameCharacters()
    return Paths.get(directoryName).createDir()
  }

  fun parseOutputOptions(opts: CmdOpts): OutputOptions {
    val verificationReportsDirectory = getVerificationReportsDirectory(opts)
    println("Verification reports directory: $verificationReportsDirectory")
    val teamCityLog = if (opts.needTeamCityLog) TeamCityLog(System.out) else null
    return OutputOptions(
        verificationReportsDirectory,
        teamCityLog,
        TeamCityResultPrinter.GroupBy.parse(opts.teamCityGroupType),
        opts.dumpBrokenPluginsFile
    )
  }

  fun createIdeDescriptor(idePath: Path, opts: CmdOpts): IdeDescriptor {
    val ideVersion = takeVersionFromCmd(opts)
    return IdeDescriptor.create(idePath, ideVersion, null)
  }

  fun getJdkPath(opts: CmdOpts): Path {
    val runtimeDirectory = opts.runtimeDir
    val jdkPath = if (runtimeDirectory != null) {
      Paths.get(runtimeDirectory)
    } else {
      val javaHome = System.getenv("JAVA_HOME")
      requireNotNull(javaHome) { "JAVA_HOME is not specified" }
      Paths.get(javaHome)
    }
    require(jdkPath.isDirectory) { "Invalid JDK path: $jdkPath" }
    return jdkPath
  }

  private fun takeVersionFromCmd(opts: CmdOpts): IdeVersion? {
    val build = opts.actualIdeVersion
    if (!build.isNullOrBlank()) {
      return IdeVersion.createIdeVersionIfValid(build)
          ?: throw IllegalArgumentException("Incorrect update IDE-version has been specified $build")
    }
    return null
  }

  fun getExternalClassesPackageFilter(opts: CmdOpts): PackageFilter =
      opts.externalClassesPrefixes
          .map { it.replace('.', '/') }
          .let { list ->
            PackageFilter(list.map { PackageFilter.Descriptor(true, it) })
          }

  private fun createIgnoredProblemsFilter(opts: CmdOpts): ProblemsFilter? {
    if (opts.ignoreProblemsFile != null) {
      val file = File(opts.ignoreProblemsFile!!)
      if (!file.exists()) {
        throw IllegalArgumentException("Ignored problems file doesn't exist $file")
      }
      return getIgnoreFilter(file)
    }
    return null
  }

  /**
   * Determines which subsystem should be verified in this task.
   *
   * Whether we would like to track only IDEA related problems (-without-android),
   * or only Android related problems (MP-1377) (-android-only),
   * or both IDEA and Android problems (-all).
   */
  private fun createSubsystemProblemsFilter(opts: CmdOpts) =
      when (opts.subsystemsToCheck) {
        "android-only" -> AndroidProblemsFilter()
        "without-android" -> IdeaOnlyProblemsFilter()
        else -> null
      }

  fun getProblemsFilters(opts: CmdOpts): List<ProblemsFilter> {
    val ignoredProblemsFilter = createIgnoredProblemsFilter(opts)
    val documentedProblemsFilter = safeCreateDocumentedProblemsFilter(opts)
    val codeProblemsFilter = createSubsystemProblemsFilter(opts)
    return listOfNotNull(ignoredProblemsFilter) +
        listOfNotNull(documentedProblemsFilter) +
        listOfNotNull(codeProblemsFilter)
  }

  private fun safeCreateDocumentedProblemsFilter(opts: CmdOpts) = try {
    DocumentedProblemsFilter.createFilter(opts.documentedProblemsPageUrl)
  } catch (e: Exception) {
    e.rethrowIfInterrupted()
    LOG.error(
        "Failed to fetch documented problems page ${opts.documentedProblemsPageUrl}. " +
            "The problems described on the page will not be ignored.", e
    )
    null
  }

  private fun getIgnoreFilter(ignoreProblemsFile: File): IgnoredProblemsFilter {
    val ignoreConditions = arrayListOf<IgnoreCondition>()
    try {
      ignoreProblemsFile.useLines { lines ->
        for (line in lines.map { it.trim() }) {
          if (line.isBlank() || line.startsWith("//")) {
            //it is a comment
            continue
          }

          ignoreConditions.add(IgnoreCondition.parseCondition(line))
        }
      }
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      throw IllegalArgumentException("Unable to parse ignored problems file $ignoreProblemsFile", e)
    }

    return IgnoredProblemsFilter(ignoreConditions)
  }

  /**
   * Parses set of excluded plugins from [CmdOpts.excludedPluginsFile] file,
   * which is a set of pairs of `(<plugin-id>, <version>)`.
   */
  fun parseExcludedPlugins(opts: CmdOpts): Set<PluginIdAndVersion> {
    val epf = opts.excludedPluginsFile ?: return emptySet()
    return IdeResourceUtil.readBrokenPluginsFromFile(File(epf))
  }

}