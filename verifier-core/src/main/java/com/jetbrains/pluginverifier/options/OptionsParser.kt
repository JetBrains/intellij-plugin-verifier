package com.jetbrains.pluginverifier.options

import com.google.common.collect.HashMultimap
import com.jetbrains.plugin.structure.classes.resolvers.JarFileResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.misc.singletonOrEmpty
import com.jetbrains.pluginverifier.output.settings.dependencies.AllMissingDependencyIgnoring
import com.jetbrains.pluginverifier.output.settings.dependencies.MissingDependencyIgnoring
import com.jetbrains.pluginverifier.output.settings.dependencies.SpecifiedMissingDependencyIgnoring
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import com.jetbrains.pluginverifier.parameters.filtering.DocumentedProblemsFilter
import com.jetbrains.pluginverifier.parameters.filtering.IgnoredProblemsFilter
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.parameters.filtering.documented.DocumentedProblemsFetcher
import com.jetbrains.pluginverifier.parameters.filtering.documented.DocumentedProblemsParser
import com.jetbrains.pluginverifier.parameters.ide.IdeCreator
import com.jetbrains.pluginverifier.parameters.ide.IdeDescriptor
import com.jetbrains.pluginverifier.repository.PluginIdAndVersion
import com.jetbrains.pluginverifier.tasks.OutputOptions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.*

object OptionsParser {

  private val LOG: Logger = LoggerFactory.getLogger(OptionsParser::class.java)

  private val TIMESTAMP_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

  fun getVerificationReportsDirectory(opts: CmdOpts): File? {
    return opts.verificationReportsDir?.let { File(it) }
  }

  private fun createFallback(): File {
    val timestamp = TIMESTAMP_DATE_FORMAT.format(Date())
    return File("verification-$timestamp")
  }

  fun parseOutputOptions(opts: CmdOpts): OutputOptions = OutputOptions(
      createMissingDependencyIgnorer(opts),
      opts.needTeamCityLog,
      TeamCityResultPrinter.GroupBy.parse(opts.teamCityGroupType),
      opts.htmlReportFile?.let { File(it) },
      opts.dumpBrokenPluginsFile
  )

  private fun createMissingDependencyIgnorer(opts: CmdOpts): MissingDependencyIgnoring {
    if (opts.ignoreAllMissingOptionalDeps) {
      return AllMissingDependencyIgnoring
    }
    return SpecifiedMissingDependencyIgnoring(opts.ignoreMissingOptionalDeps.toSet())
  }

  fun createIdeDescriptor(ideToCheckFile: File, opts: CmdOpts): IdeDescriptor {
    val ideVersion = takeVersionFromCmd(opts)
    return IdeCreator.createByFile(ideToCheckFile, ideVersion)
  }

  private fun takeVersionFromCmd(opts: CmdOpts): IdeVersion? {
    val build = opts.actualIdeVersion
    if (!build.isNullOrBlank()) {
      try {
        return IdeVersion.createIdeVersion(build!!)
      } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("Incorrect update IDE-version has been specified " + build, e)
      }

    }
    return null
  }

  fun getJdkDir(opts: CmdOpts): File {
    val runtimeDirectory: File

    if (opts.runtimeDir != null) {
      runtimeDirectory = File(opts.runtimeDir)
      if (!runtimeDirectory.isDirectory) {
        throw RuntimeException("Specified runtime directory is not a directory: " + opts.runtimeDir)
      }
    } else {
      val javaHome = System.getenv("JAVA_HOME") ?: throw RuntimeException("JAVA_HOME is not specified")

      runtimeDirectory = File(javaHome)
      if (!runtimeDirectory.isDirectory) {
        throw RuntimeException("Invalid JAVA_HOME: " + javaHome)
      }
    }

    return runtimeDirectory
  }

  fun getExternalClassPath(opts: CmdOpts): Resolver =
      UnionResolver.create(opts.externalClasspath.map { JarFileResolver(File(it)) })

  fun getExternalClassesPrefixes(opts: CmdOpts): List<String> = opts.externalClassesPrefixes.map { it.replace('.', '/') }

  private fun createIgnoredProblemsFilter(opts: CmdOpts): ProblemsFilter? {
    if (opts.ignoreProblemsFile != null) {
      val problemsToIgnore = getProblemsToIgnoreFromFile(opts.ignoreProblemsFile!!)
      val savedIgnoredProblemsFile = getSavedIgnoredProblemsFile(opts)
      return IgnoredProblemsFilter(problemsToIgnore, savedIgnoredProblemsFile)
    }
    return null
  }

  private fun getSavedIgnoredProblemsFile(opts: CmdOpts): File? = if (opts.saveIgnoredProblemsFile != null) {
    val file = File(opts.saveIgnoredProblemsFile!!)
    file.deleteLogged()
    file
  } else {
    null
  }

  private fun createDocumentedProblemsFilter(opts: CmdOpts): ProblemsFilter? {
    if (opts.documentedProblemsPageUrl != null) {
      val documentedPage = fetchDocumentedProblemsPage(opts) ?: return null
      val documentedProblems = DocumentedProblemsParser().parse(documentedPage)
      val savedIgnoredProblemsFile = getSavedIgnoredProblemsFile(opts)
      return DocumentedProblemsFilter(documentedProblems, savedIgnoredProblemsFile)
    }
    return null
  }

  private fun fetchDocumentedProblemsPage(opts: CmdOpts): String? = try {
    DocumentedProblemsFetcher().fetchPage(opts.documentedProblemsPageUrl!!)
  } catch (e: Exception) {
    LOG.error("Failed to fetch documented problems page ${opts.documentedProblemsPageUrl}. " +
        "The problems described on the page will not be ignored.", e)
    null
  }

  fun getProblemsFilters(opts: CmdOpts): List<ProblemsFilter> {
    val ignoredProblemsFilter = createIgnoredProblemsFilter(opts)
    val documentedProblemsFilter = createDocumentedProblemsFilter(opts)
    return ignoredProblemsFilter.singletonOrEmpty() + documentedProblemsFilter.singletonOrEmpty()
  }

  /**
   * @return _(pluginXmlId, version)_ -> to be ignored _problem pattern_
   */
  private fun getProblemsToIgnoreFromFile(ignoreProblemsFile: String): HashMultimap<PluginIdAndVersion, Regex> {
    val file = File(ignoreProblemsFile)
    if (!file.exists()) {
      throw IllegalArgumentException("Ignored problems file doesn't exist " + ignoreProblemsFile)
    }

    val m = HashMultimap.create<PluginIdAndVersion, Regex>()
    try {
      BufferedReader(FileReader(file)).use { br ->
        var s: String?
        while (true) {
          s = br.readLine() ?: break
          s = s.trim { it <= ' ' }
          if (s.isEmpty() || s.startsWith("//")) continue //it is a comment

          val tokens = s.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

          if (tokens.size != 3) {
            throw IllegalArgumentException("incorrect problem line $s\nthe line must be in the form: <plugin_xml_id>:<plugin_version>:<problem_description_regexp_pattern>\n" +
                "<plugin_version> may be empty (which means that a problem will be ignored in all the versions of the plugin)\n" +
                "example: org.jetbrains.kotlin::access to unresolved class org.jetbrains.kotlin.compiler.*")
          }

          val pluginId = tokens[0].trim { it <= ' ' }
          val pluginVersion = tokens[1].trim { it <= ' ' }
          val ignorePattern = tokens[2].trim { it <= ' ' }.replace('/', '.')

          m.put(PluginIdAndVersion(pluginId, pluginVersion), Regex(ignorePattern, RegexOption.IGNORE_CASE))
        }
      }
    } catch (e: Exception) {
      throw RuntimeException("Unable to parse ignored problems file " + ignoreProblemsFile, e)
    }

    return m
  }

}