package com.jetbrains.pluginverifier.utils

import com.google.common.base.Charsets
import com.google.common.base.Joiner
import com.google.common.collect.*
import com.google.common.io.Files
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.commands.CommandHolder
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.misc.RepositoryConfiguration
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.repository.RepositoryManager
import com.jetbrains.pluginverifier.results.ProblemSet
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import org.apache.commons.io.IOUtils
import java.io.*
import java.util.*

object Util {

  val CMD_OPTIONS = Options()
      .addOption("h", "help", false, "Show help")
      .addOption("r", "runtime", true, "Path to directory containing Java runtime jars (usually rt.jar and tools.jar is sufficient)")
      .addOption("s", "skip-class-for-dup-check", true, "Class name prefixes to skip in duplicate classes check, delimited by ':'")
      .addOption("e", "external-classes", true, "Classes from external libraries. Error will not be reported if class not found. Delimited by ':'")
      .addOption("all", "check-all-plugins-with-ide", false, "Check IDE build with all compatible plugins")
      .addOption("p", "plugin-to-check", true, "A plugin id to check with IDE, plugin verifier will check ALL compatible plugin builds")
      .addOption("u", "update-to-check", true, "A plugin id to check with IDE, plugin verifier will check LAST plugin build only")
      .addOption("iv", "ide-version", true, "Version of IDE that will be tested, e.g. IU-133.439")
      .addOption("epf", "excluded-plugin-file", true, "File with list of excluded plugin builds.")
      .addOption("d", "dump-broken-plugin-list", true, "File to dump broken plugin list.")
      .addOption("report", "make-report", true, "Create a detailed report about broken plugins.")
      .addOption("xr", "save-results-xml", true, "Save results to xml file")
      .addOption("tc", "team-city-output", false, "Print TeamCity compatible output.")
      .addOption("pluginsFile", "plugins-to-check-file", true, "The file that contains list of plugins to check.")
      .addOption("cp", "external-class-path", true, "External class path")
      .addOption("printFile", true, ".xml report file to be printed in TeamCity")
      .addOption("repo", "results-repository", true, "Url of repository which contains check results")
      .addOption("g", "group", true, "Whether to group problems presentation (possible args are 'plugin' - group by plugin and 'type' - group by error-type)")
      .addOption("dce", "dont-check-excluded", false, "If specified no plugins from -epf will be checked at all")
      .addOption("imod", "ignore-missing-optional-dependencies", true, "Missing optional dependencies on the plugin IDs specified in this parameter will be ignored")
      .addOption("ip", "ignore-problems", true, "Problems specified in this file will be ignored. File must contain lines in form <plugin_xml_id>:<plugin_version>:<problem_description_regexp_pattern>")

  //TODO: write a System.option for appending this list.
  private val IDEA_ULTIMATE_MODULES = ImmutableList.of(
      "com.intellij.modules.platform",
      "com.intellij.modules.lang",
      "com.intellij.modules.vcs",
      "com.intellij.modules.xml",
      "com.intellij.modules.xdebugger",
      "com.intellij.modules.java",
      "com.intellij.modules.ultimate",
      "com.intellij.modules.all")

  fun printHelp() {
    HelpFormatter().printHelp("java -jar verifier.jar <command> [<args>]", CMD_OPTIONS)
  }

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

  fun extractPluginToCheckList(commandLine: CommandLine): Pair<List<String>, List<String>> {
    val pluginsCheckAllBuilds = ArrayList<String>()
    val pluginsCheckLastBuilds = ArrayList<String>()

    val pluginIdsCheckAllBuilds = commandLine.getOptionValues('p') //plugin-to-check
    if (pluginIdsCheckAllBuilds != null) {
      pluginsCheckAllBuilds.addAll(Arrays.asList(*pluginIdsCheckAllBuilds))
    }

    val pluginIdsCheckLastBuilds = commandLine.getOptionValues('u') //update-to-check
    if (pluginIdsCheckLastBuilds != null) {
      pluginsCheckLastBuilds.addAll(Arrays.asList(*pluginIdsCheckLastBuilds))
    }

    val pluginsFile = commandLine.getOptionValue("pluginsFile") //plugins-to-check-file (usually checkedPlugins.txt)
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
  fun getExcludedPlugins(commandLine: CommandLine): Multimap<String, String> {
    val epf = commandLine.getOptionValue("epf") ?: return ArrayListMultimap.create<String, String>() //excluded-plugin-file (usually brokenPlugins.txt)
    //no predicate specified

    //file containing list of broken plugins (e.g. IDEA-*/lib/resources.jar!/brokenPlugins.txt)
    val br = BufferedReader(FileReader(File(epf)))
    try {
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

    } finally {
      IOUtils.closeQuietly(br)
    }
  }

  @Throws(IOException::class)
  fun dumbBrokenPluginsList(dumpBrokenPluginsFile: String, brokenUpdates: Collection<UpdateInfo>) {
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
  fun saveResultsToXml(xmlFile: String,
                       ideVersion: String,
                       results: Map<UpdateInfo, ProblemSet>) {
    val problems = LinkedHashMap<UpdateInfo, Collection<Problem>>()

    for (entry in results.entries) {
      problems.put(entry.key, entry.value.allProblems)
    }

    ProblemUtils.saveProblems(File(xmlFile), ideVersion, problems)
  }

  fun isDefaultModule(moduleId: String): Boolean {
    return IDEA_ULTIMATE_MODULES.contains(moduleId)
  }

  fun failOnCyclicDependency(): Boolean {
    //TODO: change this with a method parameter
    return java.lang.Boolean.parseBoolean(RepositoryConfiguration.getInstance().getProperty("fail.on.cyclic.dependencies"))
  }

  fun loadPluginFiles(pluginToTestArg: String, ideVersion: IdeVersion): List<Pair<UpdateInfo, File>> {
    if (pluginToTestArg.startsWith("@")) {
      val pluginListFile = File(pluginToTestArg.substring(1))
      val pluginPaths: List<String>
      try {
        pluginPaths = Files.readLines(pluginListFile, Charsets.UTF_8)
      } catch (e: IOException) {
        throw RuntimeException("Cannot load plugins from " + pluginListFile.absolutePath + ": " + e.message, e)
      }

      return fetchPlugins(ideVersion, pluginListFile, pluginPaths)

    } else if (pluginToTestArg.matches("#\\d+".toRegex())) {
      val pluginId = pluginToTestArg.substring(1)
      try {
        val updateId = Integer.parseInt(pluginId)
        val updateInfo = RepositoryManager.getInstance().findUpdateById(updateId)
        val update = RepositoryManager.getInstance().getPluginFile(updateInfo!!)
        return listOf(Pair<UpdateInfo, File>(UpdateInfo(updateId), update!!))
      } catch (e: IOException) {
        throw RuntimeException("Cannot load plugin #" + pluginId, e)
      }

    } else {
      val file = File(pluginToTestArg)
      if (!file.exists()) {
        // Looks like user write unknown command. This command was called because it's default command.
        throw RuntimeException("Unknown command: " + pluginToTestArg + "\navailable commands: " + Joiner.on(", ").join(CommandHolder.getCommandMap().keys))
      }
      return listOf(Pair(updateInfoByFile(file), file))
    }
  }

  private fun fetchPlugins(ideVersion: IdeVersion, pluginListFile: File, pluginPaths: List<String>): List<Pair<UpdateInfo, File>> {
    val pluginsFiles = ArrayList<Pair<UpdateInfo, File>>()

    for (pp in pluginPaths) {
      val pluginPath = pp.trim()
      if (pluginPath.isEmpty()) continue

      if (pluginPath.startsWith("id:")) {
        //single plugin by plugin build number

        val pluginId = pluginPath.substring("id:".length)
        val pluginBuilds = downloadPluginBuilds(pluginId, ideVersion)
        if (!pluginBuilds.isEmpty()) {
          pluginsFiles.add(pluginBuilds[0])
        }

      } else if (pluginPath.startsWith("ids:")) {
        //all updates of this plugin compatible with specified IDEA

        val pluginId = pluginPath.substring("ids:".length)
        pluginsFiles.addAll(downloadPluginBuilds(pluginId, ideVersion))

      } else {
        var file = File(pluginPath)
        if (!file.isAbsolute) {
          file = File(pluginListFile.parentFile, pluginPath)
        }
        if (!file.exists()) {
          throw RuntimeException("Plugin file '" + pluginPath + "' specified in '" + pluginListFile.absolutePath + "' doesn't exist")
        }

        pluginsFiles.add(Pair(updateInfoByFile(file), file))
      }
    }

    return pluginsFiles
  }

  private fun updateInfoByFile(file: File): UpdateInfo {
    var name = file.name
    val idx = name.lastIndexOf('.')
    if (idx != -1) {
      name = name.substring(0, idx)
    }
    if (name.matches("\\d+".toRegex())) {
      return UpdateInfo(Integer.parseInt(name))
    }
    return UpdateInfo(name, name, "?")
  }

  private fun downloadPluginBuilds(pluginId: String, ideVersion: IdeVersion): List<Pair<UpdateInfo, File>> {
    val compatibleUpdatesForPlugins: List<UpdateInfo>
    try {
      compatibleUpdatesForPlugins = RepositoryManager.getInstance().getAllCompatibleUpdatesOfPlugin(ideVersion, pluginId)
    } catch (e: IOException) {
      throw RuntimeException("Failed to fetch list of $pluginId versions", e)
    }

    val result = ArrayList<Pair<UpdateInfo, File>>()
    for (updateInfo in compatibleUpdatesForPlugins) {
      try {
        result.add(Pair<UpdateInfo, File>(updateInfo, RepositoryManager.getInstance().getPluginFile(updateInfo)!!))
      } catch (e: IOException) {
        throw RuntimeException("Cannot download '" + updateInfo, e)
      }

    }
    return result
  }
}
