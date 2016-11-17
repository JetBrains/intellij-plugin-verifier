package com.jetbrains.pluginverifier.utils

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.IdeManager
import com.intellij.structure.domain.IdeVersion
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VOptions
import com.jetbrains.pluginverifier.output.VPrinterOptions
import com.sampullara.cli.Argument
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.regex.Pattern

open class PublicOpts(
    @set:Argument("runtime-dir", alias = "r", description = "The path to directory containing Java runtime jars (e.g. /usr/lib/jvm/java-8-oracle ")
    var runtimeDir: String? = null,

    @set:Argument("team-city", alias = "tc", description = "Specify this flag if you want to print the TeamCity compatible output on stdout.")
    var needTeamCityLog: Boolean = false,

    @set:Argument("tc-grouping", alias = "g", description = "How to group the TeamCity presentation of the problems: either 'plugin' to group by each plugin or 'problem_type' to group by problem type")
    var group: String? = null,

    @set:Argument("plugins-to-check-all-builds", alias = "p-all", delimiter = ":", description = "The plugin ids to check with IDE. The plugin verifier will check ALL compatible plugin builds")
    var pluginToCheckAllBuilds: Array<String> = arrayOf(),

    @set:Argument("plugins-to-check-last-builds", alias = "p-last", delimiter = ":", description = "The plugin ids to check with IDE. The plugin verifier will check LAST plugin build only")
    var pluginToCheckLastBuild: Array<String> = arrayOf(),

    @set:Argument("excluded-plugins-file", alias = "epf", description = "File with list of excluded plugin builds (e.g. '<IDE-home>/lib/resources.jar/brokenPlugins.txt')")
    var excludedPluginsFile: String? = null,

    @set:Argument("dump-broken-plugin-list", alias = "d", description = "File to dump broken plugin ids. The broken plugins are those which contain at least one problem as a result of the verification")
    var dumpBrokenPluginsFile: String? = null,

    @set:Argument("html-report", description = "Create HTML report of broken plugins")
    var htmlReportFile: String? = null,

    @set:Argument("plugins-to-check-file", alias = "ptcf", description = "File that contains list of plugins to check (e.g. '<IDE-home>/lib/resources.jar/checkedPlugins.txt')")
    var pluginsToCheckFile: String? = null,

    @set:Argument("external-prefixes", alias = "ex-prefixes", delimiter = ":", description = "The prefixes of classes from the external libraries. The Verifier will not report 'No such class' for such classes.")
    var externalClassesPrefixes: Array<String> = arrayOf()
)

open class CmdOpts(
    @set:Argument("ignored-problems", alias = "ip", description = "The problems specified in this file will be ignored. The file must contain lines in form <plugin_xml_id>:<plugin_version>:<problem_description_regexp_pattern>")
    var ignoreProblemsFile: String? = null,

    @set:Argument("ide-version", alias = "iv", description = "The actual version of the IDE that will be verified. This value will overwrite the one found in the IDE itself")
    var actualIdeVersion: String? = null,

    @set:Argument("external-classpath", alias = "ex-cp", delimiter = ":", description = "The classes from external libraries. The Verifier will not report 'No such class' for such classes.")
    var externalClasspath: Array<String> = arrayOf(),

    @set:Argument("save-check-ide-report", alias = "save", description = "Save the check IDE report to this file")
    var saveCheckIdeReport: String? = null,

    @set:Argument("ignore-all-missing-optional-dependencies", alias = "ignore-all-missing-opt-deps", description = "If specified, all the optional missing plugins will not be treated as problems")
    var ignoreAllMissingOptionalDeps: Boolean = false,

    @set:Argument("ignore-specific-missing-optional-dependencies", alias = "ignore-specific-missing-opt-deps", delimiter = ":")
    var ignoreMissingOptionalDeps: Array<String> = arrayOf()

) : PublicOpts()

object CmdUtil {


  @Throws(IOException::class)
  fun createIde(ideToCheck: File, opts: CmdOpts): Ide {
    return IdeManager.getInstance().createIde(ideToCheck, takeVersionFromCmd(opts))
  }

  @Throws(IOException::class)
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

  @Throws(IOException::class)
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

  @Throws(IOException::class)
  fun getExternalClassPath(opts: CmdOpts): Resolver =
      Resolver.createUnionResolver("External classpath resolver: ${opts.externalClasspath}",
          opts.externalClasspath.map { Resolver.createJarResolver(File(it)) })


}

object VOptionsUtil {

  @JvmStatic
  fun parsePrinterOptions(opts: CmdOpts): VPrinterOptions = VPrinterOptions(opts.ignoreAllMissingOptionalDeps, opts.ignoreMissingOptionalDeps.toList())

  @JvmStatic
  fun parseOpts(opts: CmdOpts): VOptions {

    var problemsToIgnore: Multimap<Pair<String, String>, Pattern> = HashMultimap.create<Pair<String, String>, Pattern>()

    val ignoreProblemsFile = opts.ignoreProblemsFile
    if (ignoreProblemsFile != null) {
      problemsToIgnore = getProblemsToIgnoreFromFile(ignoreProblemsFile)
    }

    return VOptions(
        opts.externalClassesPrefixes.map { it.replace('.', '/') }.toSet(),
        problemsToIgnore
    )
  }

  fun getProblemsToIgnoreFromFile(ignoreProblemsFile: String): Multimap<Pair<String, String>, Pattern> {
    val file = File(ignoreProblemsFile)
    if (!file.exists()) {
      throw IllegalArgumentException("Ignored problems file doesn't exist " + ignoreProblemsFile)
    }

    val m = HashMultimap.create<Pair<String, String>, Pattern>()
    try {
      BufferedReader(FileReader(file)).use { br ->
        var s: String?
        while (true) {
          s = br.readLine() ?: break
          s = s.trim { it <= ' ' }
          if (s.isEmpty() || s.startsWith("//")) continue //it is a comment

          val tokens = s.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

          if (tokens.size != 3) {
            throw IllegalArgumentException("incorrect problem line $s\nthe line must be in the form: <plugin_xml_id>:<plugin_version>:<problem_description_regexp_pattern>\n<plugin_version> may be empty (which means that a problem will be ignored in all the versions of the plugin)\nexample 'org.jetbrains.kotlin::accessing to unknown class org/jetbrains/kotlin/compiler/.*' - ignore all the missing classes from org.jetbrains.kotlin.compiler package")
          }

          val pluginId = tokens[0].trim { it <= ' ' }
          val pluginVersion = tokens[1].trim { it <= ' ' }
          val ignorePattern = tokens[2].trim { it <= ' ' }.replace('/', '.')

          m.put(pluginId to pluginVersion, Pattern.compile(ignorePattern))
        }
      }
    } catch (e: Exception) {
      throw RuntimeException("Unable to parse ignored problems file " + ignoreProblemsFile, e)
    }

    return m
  }

}
