package com.jetbrains.pluginverifier.options

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.jetbrains.plugin.structure.classes.resolvers.JarFileResolver
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
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
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

object OptionsParser {

  private val LOG: Logger = LoggerFactory.getLogger(OptionsParser::class.java)

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
    val directoryName = ("verification-" + nowTime).replaceInvalidFileNameCharacters()
    return Paths.get(directoryName).createDir()
  }

  fun parseOutputOptions(opts: CmdOpts, verificationReportsDirectory: Path) = OutputOptions(
      createMissingDependencyIgnorer(opts),
      opts.needTeamCityLog,
      TeamCityResultPrinter.GroupBy.parse(opts.teamCityGroupType),
      opts.dumpBrokenPluginsFile,
      verificationReportsDirectory
  )

  private fun createMissingDependencyIgnorer(opts: CmdOpts): MissingDependencyIgnoring {
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
          ?: throw IllegalArgumentException("Incorrect update IDE-version has been specified " + build)
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
        throw RuntimeException("Invalid JAVA_HOME: " + javaHome)
      }
    }

    return runtimeDirectory
  }

  fun getExternalClassPath(opts: CmdOpts) = UnionResolver.create(opts.externalClasspath.map { JarFileResolver(File(it)) })

  fun getExternalClassesPrefixes(opts: CmdOpts) = opts.externalClassesPrefixes.map { it.replace('.', '/') }

  private fun createIgnoredProblemsFilter(opts: CmdOpts): ProblemsFilter? {
    if (opts.ignoreProblemsFile != null) {
      val problemsToIgnore = getProblemsToIgnoreFromFile(opts.ignoreProblemsFile!!)
      return IgnoredProblemsFilter(problemsToIgnore)
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
   * Determines whether we would like to track only IDEA-related problems,
   * or only Android-related problems (MP-1377), or both IDEA and Android problems.
   */
  private fun createIdeaOrAndroidProblemsFilter(opts: CmdOpts): ProblemsFilter? {
    if (opts.checkAndroidOnly && opts.checkIdeaOnly) {
      throw IllegalArgumentException("Only one option -idea or -android can be specified")
    }
    if (opts.checkIdeaOnly) {
      return AndroidProblemsFilter()
    }
    if (opts.checkAndroidOnly) {
      return IdeaProblemsFilter()
    }
    return null
  }

  fun getProblemsFilters(opts: CmdOpts): List<ProblemsFilter> {
    val ignoredProblemsFilter = createIgnoredProblemsFilter(opts)
    val documentedProblemsFilter = createDocumentedProblemsFilter(opts)
    val codeProblemsFilter = createIdeaOrAndroidProblemsFilter(opts)
    return ignoredProblemsFilter.singletonOrEmpty() +
        documentedProblemsFilter.singletonOrEmpty() +
        codeProblemsFilter.singletonOrEmpty()
  }

  /**
   * @return _(pluginXmlId, version)_ -> to be ignored _problem pattern_
   */
  private fun getProblemsToIgnoreFromFile(ignoreProblemsFile: String): Multimap<PluginIdAndVersion, Regex> {
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

  /**
   * Parses a file containing a list of plugin IDs to check.
   * ```
   * plugin.one
   * $plugin.two
   * //comment
   * plugin.three$
   * ```
   *
   * If '$' is specified as a prefix or a suffix, only the last version
   * of the plugin will be checked. Otherwise, all versions of the plugin will be checked.
   *
   * Returns (ID-s of plugins to check all builds, ID-s of plugins to check last builds)
   */
  fun parseAllAndLastPluginIdsToCheck(opts: CmdOpts): Pair<List<String>, List<String>> {
    val pluginsCheckAllBuilds = arrayListOf<String>()
    val pluginsCheckLastBuilds = arrayListOf<String>()

    pluginsCheckAllBuilds.addAll(opts.pluginToCheckAllBuilds)
    pluginsCheckLastBuilds.addAll(opts.pluginToCheckLastBuild)

    val pluginsFile = opts.pluginsToCheckFile
    if (pluginsFile != null) {
      try {
        File(pluginsFile).readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("//") }
            .forEach { fullLine ->
              val trimmed = fullLine.trim('$').trim()
              if (trimmed.isNotEmpty()) {
                if (fullLine.startsWith("$") || fullLine.endsWith("$")) {
                  pluginsCheckLastBuilds.add(trimmed)
                } else {
                  pluginsCheckAllBuilds.add(trimmed)
                }
              }
            }
      } catch (e: IOException) {
        throw RuntimeException("Failed to read plugins to check file " + pluginsFile + ": " + e.message, e)
      }
    }

    return pluginsCheckAllBuilds to pluginsCheckLastBuilds
  }

  /**
   * Requests the plugins' information for plugins with specified id-s.
   *
   * Parameter [ideVersion] is used to select the compatible versions of these plugins, that is,
   * only the updates whose [since; until] range contains the [ideVersion] will be selected.
   * [checkAllBuildsPluginIds] is a list of plugin id-s of plugins for which every build (aka plugin version) will be selected.
   * [checkLastBuildsPluginIds] is a list of plugin id-s of plugins for which only the newest build (version) will be selected.
   */
  fun requestUpdatesToCheckByIds(checkAllBuildsPluginIds: List<String>,
                                 checkLastBuildsPluginIds: List<String>,
                                 ideVersion: IdeVersion,
                                 pluginRepository: PluginRepository): List<PluginInfo> {
    if (checkAllBuildsPluginIds.isEmpty() && checkLastBuildsPluginIds.isEmpty()) {
      return pluginRepository.getLastCompatiblePlugins(ideVersion)
    } else {
      val result = arrayListOf<PluginInfo>()

      checkAllBuildsPluginIds.flatMapTo(result) {
        pluginRepository.getAllCompatibleVersionsOfPlugin(ideVersion, it)
      }

      checkLastBuildsPluginIds.distinct().mapNotNullTo(result) {
        pluginRepository.getAllCompatibleVersionsOfPlugin(ideVersion, it)
            .sortedByDescending { (it as UpdateInfo).updateId }
            .firstOrNull()
      }

      return result
    }
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