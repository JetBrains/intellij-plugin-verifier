package com.jetbrains.pluginverifier.options

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.intellij.structure.ide.IdeVersion
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.ProblemsFilter
import com.jetbrains.pluginverifier.ide.IdeCreator
import com.jetbrains.pluginverifier.output.PrinterOptions
import com.jetbrains.pluginverifier.tasks.PluginIdAndVersion
import com.jetbrains.pluginverifier.utils.IgnoredProblemsFilter
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.file.Files
import java.util.regex.Pattern

object OptionsParser {

  fun parsePrinterOptions(opts: CmdOpts): PrinterOptions = PrinterOptions(
      opts.ignoreAllMissingOptionalDeps,
      opts.ignoreMissingOptionalDeps.toList(),
      opts.needTeamCityLog,
      opts.teamCityGroupType,
      opts.htmlReportFile,
      opts.dumpBrokenPluginsFile
  )

  fun createIdeDescriptor(ideToCheckFile: File, opts: CmdOpts): IdeDescriptor {
    val ideVersion = takeVersionFromCmd(opts)
    return IdeCreator.createByFile(ideToCheckFile, ideVersion)
  }

  fun takeVersionFromCmd(opts: CmdOpts): IdeVersion? {
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
      Resolver.createUnionResolver("External classpath resolver: ${opts.externalClasspath}",
          opts.externalClasspath.map { Resolver.createJarResolver(File(it)) })

  fun getExternalClassesPrefixes(opts: CmdOpts): List<String> = opts.externalClassesPrefixes.map { it.replace('.', '/') }

  fun getProblemsFilter(opts: CmdOpts): ProblemsFilter {

    val ignoreProblemsFile = opts.ignoreProblemsFile
    val problemsToIgnore = if (ignoreProblemsFile != null) {
      getProblemsToIgnoreFromFile(ignoreProblemsFile)
    } else {
      HashMultimap.create()
    }

    val saveIgnoredProblemsFile = if (opts.saveIgnoredProblemsFile != null) File(opts.saveIgnoredProblemsFile) else null
    if (saveIgnoredProblemsFile != null) {
      try {
        Files.deleteIfExists(saveIgnoredProblemsFile.toPath())
      } catch(e: Exception) {
        throw RuntimeException("Unable to clean the file $saveIgnoredProblemsFile", e)
      }
    }

    return IgnoredProblemsFilter(problemsToIgnore, saveIgnoredProblemsFile)
  }

  /**
   * @return _(pluginXmlId, version)_ -> to be ignored _problem pattern_
   */
  private fun getProblemsToIgnoreFromFile(ignoreProblemsFile: String): Multimap<PluginIdAndVersion, Pattern> {
    val file = File(ignoreProblemsFile)
    if (!file.exists()) {
      throw IllegalArgumentException("Ignored problems file doesn't exist " + ignoreProblemsFile)
    }

    val m = HashMultimap.create<PluginIdAndVersion, Pattern>()
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

          m.put(PluginIdAndVersion(pluginId, pluginVersion), Pattern.compile(ignorePattern))
        }
      }
    } catch (e: Exception) {
      throw RuntimeException("Unable to parse ignored problems file " + ignoreProblemsFile, e)
    }

    return m
  }

}