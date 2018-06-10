package com.jetbrains.pluginverifier.options

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.ide.IdeDescriptorCreator
import com.jetbrains.pluginverifier.ide.IdeResourceUtil
import com.jetbrains.pluginverifier.misc.*
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.output.settings.dependencies.AllMissingDependencyIgnoring
import com.jetbrains.pluginverifier.output.settings.dependencies.MissingDependencyIgnoring
import com.jetbrains.pluginverifier.output.settings.dependencies.SpecifiedMissingDependencyIgnoring
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import com.jetbrains.pluginverifier.parameters.filtering.*
import com.jetbrains.pluginverifier.parameters.filtering.documented.DocumentedProblemsPagesFetcher
import com.jetbrains.pluginverifier.parameters.filtering.documented.DocumentedProblemsParser
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

  fun createIdeDescriptor(ideToCheckFile: Path, opts: CmdOpts): IdeDescriptor {
    val ideVersion = takeVersionFromCmd(opts)
    return IdeDescriptorCreator.createByPath(ideToCheckFile, ideVersion)
  }

  fun getJdkPath(opts: CmdOpts) = JdkPath(getJdkHomeDir(opts))

  private fun takeVersionFromCmd(opts: CmdOpts): IdeVersion? {
    val build = opts.actualIdeVersion
    if (!build.isNullOrBlank()) {
      return IdeVersion.createIdeVersionIfValid(build!!)
          ?: throw IllegalArgumentException("Incorrect update IDE-version has been specified $build")
    }
    return null
  }

  private fun getJdkHomeDir(opts: CmdOpts): Path {
    val runtimeDirectory: Path

    if (opts.runtimeDir != null) {
      runtimeDirectory = Paths.get(opts.runtimeDir)
      if (!runtimeDirectory.isDirectory) {
        throw RuntimeException("Specified runtime directory is not a directory: " + opts.runtimeDir)
      }
    } else {
      val javaHome = System.getenv("JAVA_HOME") ?: throw RuntimeException("JAVA_HOME is not specified")

      runtimeDirectory = Paths.get(javaHome)
      if (!runtimeDirectory.isDirectory) {
        throw RuntimeException("Invalid JAVA_HOME: $javaHome")
      }
    }

    return runtimeDirectory
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

  private fun createDocumentedProblemsFilter(opts: CmdOpts): ProblemsFilter? {
    val documentedProblemsPageUrl = opts.documentedProblemsPageUrl
    if (documentedProblemsPageUrl != null) {
      val documentedPages = fetchDocumentedProblemsPages(documentedProblemsPageUrl) ?: return null
      val documentedProblemsParser = DocumentedProblemsParser()
      val documentedProblems = documentedPages.flatMap { documentedProblemsParser.parse(it) }
      return DocumentedProblemsFilter(documentedProblems)
    }
    return null
  }

  private fun fetchDocumentedProblemsPages(mainPageUrl: String) = try {
    DocumentedProblemsPagesFetcher().fetchPages(mainPageUrl)
  } catch (e: Exception) {
    LOG.error("Failed to fetch documented problems page $mainPageUrl. " +
        "The problems described on the page will not be ignored.", e)
    null
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
    val documentedProblemsFilter = createDocumentedProblemsFilter(opts)
    val codeProblemsFilter = createSubsystemProblemsFilter(opts)
    return ignoredProblemsFilter.singletonOrEmpty() +
        documentedProblemsFilter.singletonOrEmpty() +
        codeProblemsFilter.singletonOrEmpty()
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

          val tokens = line.split(":").map { it.trim() }
          val parseRegexp = { s: String -> Regex(s, RegexOption.IGNORE_CASE) }

          ignoreConditions.add(when {
            tokens.size == 1 -> IgnoreCondition(null, null, parseRegexp(tokens[0]))
            tokens.size == 2 -> IgnoreCondition(tokens[0], null, parseRegexp(tokens[1]))
            tokens.size == 3 -> IgnoreCondition(tokens[0].takeIf { it.isNotEmpty() }, tokens[1].takeIf { it.isNotEmpty() }, parseRegexp(tokens[2]))
            else -> throw incorrectIgnoredProblemLineException(line)
          })
        }
      }
    } catch (e: Exception) {
      throw IllegalArgumentException("Unable to parse ignored problems file $ignoreProblemsFile", e)
    }

    return IgnoredProblemsFilter(ignoreConditions)
  }

  private fun incorrectIgnoredProblemLineException(line: String) = IllegalArgumentException(
      """Incorrect problem ignoring line
$line
the line must be in the form: [<plugin_xml_id>[:<plugin_version>]:]<problem_description_regexp_pattern>
Examples:
org.some.plugin:3.4.0:access to unresolved class org.foo.Foo.*                    --- ignore for plugin 'org.some.plugin' of version 3.4.0
org.jetbrains.kotlin::access to unresolved class org.jetbrains.kotlin.compiler.*  --- ignore for all versions of Kotlin plugin
access to unresolved class org.jetbrains.kotlin.compiler.*                        --- ignore for all plugins
""")

  /**
   * Parses set of excluded plugins from [CmdOpts.excludedPluginsFile] file,
   * which is a set of pairs of `(<plugin-id>, <version>)`.
   */
  fun parseExcludedPlugins(opts: CmdOpts): Set<PluginIdAndVersion> {
    val epf = opts.excludedPluginsFile ?: return emptySet()
    return IdeResourceUtil.readBrokenPluginsFromFile(File(epf))
  }

}