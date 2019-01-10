package com.jetbrains.pluginverifier.options

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.ide.IdeResourceUtil
import com.jetbrains.pluginverifier.misc.createDir
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.misc.replaceInvalidFileNameCharacters
import com.jetbrains.pluginverifier.misc.singletonOrEmpty
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.output.settings.dependencies.AllMissingDependencyIgnoring
import com.jetbrains.pluginverifier.output.settings.dependencies.MissingDependencyIgnoring
import com.jetbrains.pluginverifier.output.settings.dependencies.SpecifiedMissingDependencyIgnoring
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import com.jetbrains.pluginverifier.parameters.filtering.*
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
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

  fun getVerificationReportsDirectory(opts: CmdOpts): Path {
    val dir = opts.verificationReportsDir?.let { File(it) }
    if (dir != null) {
      if (dir.exists() && dir.listFiles().orEmpty().isNotEmpty()) {
        LOG.info("Delete the verification directory ${dir.absolutePath} because it isn't empty")
        dir.deleteLogged()
      }
      dir.createDir()
    }
    val nowTime = TIMESTAMP_DATE_FORMAT.format(Date())
    val directoryName = ("verification-$nowTime").replaceInvalidFileNameCharacters()
    return Paths.get(directoryName).createDir()
  }

  fun parseOutputOptions(opts: CmdOpts, verificationReportsDirectory: Path) = OutputOptions(
      createMissingDependencyIgnoring(opts),
      opts.needTeamCityLog,
      TeamCityResultPrinter.GroupBy.parse(opts.teamCityGroupType),
      opts.dumpBrokenPluginsFile,
      verificationReportsDirectory
  )

  private fun createMissingDependencyIgnoring(opts: CmdOpts): MissingDependencyIgnoring {
    if (opts.ignoreAllMissingOptionalDeps) {
      return AllMissingDependencyIgnoring
    }
    return SpecifiedMissingDependencyIgnoring(opts.ignoreMissingOptionalDeps.toSet())
  }

  fun createIdeDescriptor(idePath: Path, opts: CmdOpts): IdeDescriptor {
    val ideVersion = takeVersionFromCmd(opts)
    return IdeDescriptor.create(idePath, ideVersion, null)
  }

  fun getJdkPath(opts: CmdOpts): JdkPath {
    val path = opts.runtimeDir
    return if (path == null) JdkPath.createJavaHomeJdkPath() else JdkPath.createJdkPath(path)
  }

  private fun takeVersionFromCmd(opts: CmdOpts): IdeVersion? {
    val build = opts.actualIdeVersion
    if (!build.isNullOrBlank()) {
      return IdeVersion.createIdeVersionIfValid(build!!)
          ?: throw IllegalArgumentException("Incorrect update IDE-version has been specified $build")
    }
    return null
  }

  fun getExternalClassesPackageFilter(opts: CmdOpts): PackageFilter =
      opts.externalClassesPrefixes
          .map { it.replace('.', '/') }
          .let {
            PackageFilter(it.map { PackageFilter.Descriptor(true, it) })
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
    return ignoredProblemsFilter.singletonOrEmpty() +
        documentedProblemsFilter.singletonOrEmpty() +
        codeProblemsFilter.singletonOrEmpty()
  }

  private fun safeCreateDocumentedProblemsFilter(opts: CmdOpts) = try {
    DocumentedProblemsFilter.createFilter(opts.documentedProblemsPageUrl)
  } catch (ie: InterruptedException) {
    throw ie
  } catch (e: Exception) {
    LOG.error("Failed to fetch documented problems page ${opts.documentedProblemsPageUrl}. " +
        "The problems described on the page will not be ignored.", e)
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
    } catch (ie: InterruptedException) {
      throw ie
    } catch (e: Exception) {
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