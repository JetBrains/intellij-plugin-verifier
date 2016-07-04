package com.jetbrains.pluginverifier.utils

import com.google.common.base.Joiner
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.HashMultimap
import com.google.common.collect.Iterables
import com.google.common.collect.Multimap
import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.IdeManager
import com.intellij.structure.domain.IdeVersion
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VOptions
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import java.io.*
import java.util.*
import java.util.regex.Pattern

fun main(args: Array<String>) {
  val opts = Opts()
  Args.parse(opts, args)
  println(opts)
}

data class Opts(
    @set:Argument("runtime-dir", alias = "r", description = "The path to directory containing Java runtime jars (usually rt.jar and tools.jar are sufficient)")
    var runtimeDir: String? = null,

    @set:Argument("TeamCity", alias = "tc", description = "Specify this flag if you want to print the TeamCity compatible output.")
    var needTeamCityLog: Boolean = false,

    @set:Argument("tc-grouping", alias = "g", description = "How to group the TeamCity presentation of the problems")
    var group: String? = null,

    @set:Argument("ignored-problems", alias = "ip", description = "The problems specified in this file will be ignored. File must contain lines in form <plugin_xml_id>:<plugin_version>:<problem_description_regexp_pattern>")
    var ignoreProblemsFile: String? = null,

    @set:Argument("plugin-to-check", alias = "p", description = "A plugin id to check with IDE, plugin verifier will check ALL compatible plugin builds")
    var pluginsToCheck: Array<String> = arrayOf(),

    @set:Argument("update-to-check", alias = "u", description = "A plugin id to check with IDE, plugin verifier will check LAST plugin build only")
    var updatesToCheck: Array<String> = arrayOf(),

    @set:Argument("ide-version", alias = "iv", description = "Version of IDE that will be tested, e.g. IU-133.439")
    var actualIdeVersion: String? = null,

    @set:Argument("excluded-plugin-file", alias = "epf", description = "File with list of excluded plugin builds.")
    var excludedPluginsFile: String? = null,

    @set:Argument("dump-broken-plugin-list", alias = "d", description = "File to dump broken plugin list.")
    var dumpBrokenPluginsFile: String? = null,

    @set:Argument("html-report", description = "Create a beautiful HTML report of broken plugins")
    var htmlReportFile: String? = null,

    @set:Argument("results-file", description = "Save results to this file")
    var resultFile: String? = null,

    @set:Argument("plugins-to-check-file", alias = "ptcf", description = "The file that contains list of plugins to check.")
    var pluginsToCheckFile: String? = null,

    @set:Argument("ignore-missing-optional-dependencies", alias = "imod", description = "Missing optional dependencies on the plugin IDs specified in this parameter will be ignored")
    var ignoreMissingOptionalDependencies: Array<String> = arrayOf(),

    @set:Argument("externalClasspath", alias = "ex-cp", delimiter = ":", description = "Classes from external libraries. Error will not be reported if class not found. Delimited by ':'")
    var externalClasspath: Array<String> = arrayOf(),


    @set:Argument("external-prefixes", alias = "ex-prefixes", delimiter = ":", description = "The classes from the external libraries. The Verifier doesn't report 'No such class' for such classes.")
    var externalClassesPrefixes: Array<String> = arrayOf()

)

object Util {

  fun getStackTrace(t: Throwable?): String {
    if (t == null) return ""
    val sw = StringWriter()
    t.printStackTrace(PrintWriter(sw))
    return sw.toString()
  }


  fun <T> concat(first: Collection<T>, second: Collection<T>): List<T> {
    val res = ArrayList<T>(first.size + second.size)
    res.addAll(first)
    res.addAll(second)
    return res
  }

  /**
   * (id-s of plugins to check all builds, id-s of plugins to check last builds)
   */
  fun extractPluginToCheckList(opts: Opts): Pair<List<String>, List<String>> {
    val pluginsCheckAllBuilds = ArrayList<String>()
    val pluginsCheckLastBuilds = ArrayList<String>()

    pluginsCheckAllBuilds.addAll(opts.pluginsToCheck)
    pluginsCheckLastBuilds.addAll(opts.updatesToCheck)

    val pluginsFile = opts.pluginsToCheckFile
    if (pluginsFile != null) {
      try {
        val reader = BufferedReader(FileReader(pluginsFile))
        var s: String
        while (true) {
          s = reader.readLine()
          if (s == null) break
          s = s.trim { it <= ' ' }
          if (s.isEmpty() || s.startsWith("//")) continue

          var checkAllBuilds = true
          if (s.endsWith("$")) {
            s = s.substring(0, s.length - 1).trim { it <= ' ' }
            checkAllBuilds = false
          }
          if (s.startsWith("$")) {
            s = s.substring(1).trim { it <= ' ' }
            checkAllBuilds = false
          }

          if (checkAllBuilds) {
            pluginsCheckAllBuilds.add(s)
          } else {
            if (s.isEmpty()) continue

            pluginsCheckLastBuilds.add(s)
          }
        }
      } catch (e: IOException) {
        throw RuntimeException("Failed to read plugins file " + pluginsFile + ": " + e.message, e)
      }

    }

    println("List of plugins to check: " + Joiner.on(", ").join(Iterables.concat(pluginsCheckAllBuilds, pluginsCheckLastBuilds)))

    return Pair<List<String>, List<String>>(pluginsCheckAllBuilds, pluginsCheckLastBuilds)
  }

  @Throws(IOException::class)
  fun getExcludedPlugins(opts: Opts): Multimap<String, String> {
    val epf = opts.excludedPluginsFile ?: return ArrayListMultimap.create<String, String>() //excluded-plugin-file (usually brokenPlugins.txt)
    //no predicate specified

    //file containing list of broken plugins (e.g. IDEA-*/lib/resources.jar!/brokenPlugins.txt)
    BufferedReader(FileReader(File(epf))).use { br ->
      val m = HashMultimap.create<String, String>()

      var s: String
      while (true) {
        s = br.readLine()
        if (s == null) break
        s = s.trim { it <= ' ' }
        if (s.startsWith("//")) continue //it is a comment

        val tokens = ParametersListUtil.parse(s)
        if (tokens.isEmpty()) continue

        if (tokens.size == 1) {
          throw IOException(epf + " is broken. The line contains plugin name, but does not contain version: " + s)
        }

        val pluginId = tokens[0]

        m.putAll(pluginId, tokens.subList(1, tokens.size)) //"plugin id" -> [all its builds]
      }

      return m
    }
  }

  @Throws(IOException::class)
  fun dumbBrokenPluginsList(dumpBrokenPluginsFile: String, brokenUpdates: List<UpdateInfo>) {
    println("Dumping list of broken plugins to " + dumpBrokenPluginsFile)

    PrintWriter(dumpBrokenPluginsFile).use { out ->
      out.println("// This file contains list of broken plugins.\n" +
          "// Each line contains plugin ID and list of versions that are broken.\n" +
          "// If plugin name or version contains a space you can quote it like in command line.\n")

      for (entry in brokenUpdates.groupBy { it.pluginId!! }.entries) {

        out.print(ParametersListUtil.join(listOf(entry.key)))
        out.print("    ")
        out.println(ParametersListUtil.join(entry.value.map { it.version }.sortedWith(VersionComparatorUtil.COMPARATOR)))
      }
    }
  }

  @Throws(IOException::class)
  fun createIde(ideToCheck: File, opts: Opts): Ide {
    return IdeManager.getInstance().createIde(ideToCheck, takeVersionFromCmd(opts))
  }

  @Throws(IOException::class)
  private fun takeVersionFromCmd(opts: Opts): IdeVersion? {
    val build = opts.actualIdeVersion
    if (build != null && !build.isEmpty()) {
      try {
        return IdeVersion.createIdeVersion(build)
      } catch (e: IllegalArgumentException) {
        throw RuntimeException("Incorrect update IDE-version has been specified " + build, e)
      }

    }
    return null
  }

  @Throws(IOException::class)
  fun getJdkDir(opts: Opts): File {
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
  fun getExternalClassPath(opts: Opts): Resolver =
      Resolver.createUnionResolver("External classpath resolver: ${opts.externalClasspath}",
          opts.externalClasspath.map { Resolver.createJarResolver(File(it)) })


}

object VOptionsUtil {

  @JvmStatic
  fun parseOpts(opts: Opts): VOptions {

    var problemsToIgnore: Multimap<Pair<String, String>, Pattern> = HashMultimap.create<Pair<String, String>, Pattern>()

    val ignoreProblemsFile = opts.ignoreProblemsFile
    if (ignoreProblemsFile != null) {
      problemsToIgnore = getProblemsToIgnoreFromFile(ignoreProblemsFile)
    }

    return VOptions(opts.externalClassesPrefixes.map { it.replace('.', '/') }.toTypedArray(), opts.ignoreMissingOptionalDependencies.toSet(), problemsToIgnore)
  }

  private fun getProblemsToIgnoreFromFile(ignoreProblemsFile: String): Multimap<Pair<String, String>, Pattern> {
    val file = File(ignoreProblemsFile)
    if (!file.exists()) {
      throw IllegalArgumentException("Ignored problems file doesn't exist " + ignoreProblemsFile)
    }

    val m = HashMultimap.create<Pair<String, String>, Pattern>()
    try {
      BufferedReader(FileReader(file)).use { br ->
        var s: String
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
