package com.jetbrains.pluginverifier.utils

import com.google.common.base.Charsets
import com.google.common.base.Joiner
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.HashMultimap
import com.google.common.collect.Iterables
import com.google.common.collect.Multimap
import com.google.common.io.Files
import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.IdeManager
import com.intellij.structure.domain.IdeVersion
import com.intellij.structure.domain.Plugin
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.misc.RepositoryConfiguration
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.repository.RepositoryManager
import com.jetbrains.pluginverifier.results.ProblemSet
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.GnuParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import java.io.*
import java.util.*
import java.util.regex.Pattern

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

  /**
   * (id-s of plugins to check all builds, id-s of plugins to check last builds)
   */
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
  fun saveResultsToXml(xmlFile: File,
                       ideVersion: IdeVersion,
                       results: Map<UpdateInfo, ProblemSet>) {
    val problems = LinkedHashMap<UpdateInfo, Collection<Problem>>()

    for (entry in results.entries) {
      problems.put(entry.key, entry.value.allProblems)
    }

    ProblemUtils.saveProblems(xmlFile, ideVersion, problems)
  }

  @Throws(IOException::class)
  fun createIde(ideToCheck: File, commandLine: CommandLine): Ide {
    return IdeManager.getInstance().createIde(ideToCheck, takeVersionFromCmd(commandLine))
  }

  @Throws(IOException::class)
  private fun takeVersionFromCmd(commandLine: CommandLine): IdeVersion? {
    val build = commandLine.getOptionValue("iv")
    if (build != null && !build.isEmpty()) {
      try {
        return IdeVersion.createIdeVersion(build)
      } catch (e: IllegalArgumentException) {
        throw RuntimeException("Incorrect update IDE-version has been specified " + build, e)
      }

    }
    return null
  }

  @Throws(Exception::class)
  fun verify(plugin: Plugin,
             ide: Ide,
             ideResolver: Resolver,
             jdkDir: File,
             externalClassPath: Resolver,
             options: VOptions): ProblemSet {
    val jdkDescriptor = JdkDescriptor.ByFile(jdkDir)
    val pairs = listOf(Pair<PluginDescriptor, IdeDescriptor>(PluginDescriptor.ByInstance(plugin), IdeDescriptor.ByInstance(ide, ideResolver)))

    //the exceptions are propagated
    val result = VManager.verify(VParams(jdkDescriptor, pairs, options, externalClassPath)).results[0]

    if (result is VResult.Problems) {
      val problemSet = ProblemSet()
      result.problems.entries().forEach { x -> problemSet.addProblem(x.key, x.value) }
      return problemSet
    } else if (result is VResult.BadPlugin) {
      throw IllegalArgumentException(result.overview) //will be caught above
    }
    return ProblemSet()
  }


  @Throws(IOException::class)
  fun getJdkDir(commandLine: CommandLine): File {
    val runtimeDirectory: File

    if (commandLine.hasOption('r')) {
      runtimeDirectory = File(commandLine.getOptionValue('r'))
      if (!runtimeDirectory.isDirectory) {
        throw RuntimeException("Specified runtime directory is not a directory: " + commandLine.getOptionValue('r'))
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
  fun getExternalClassPath(commandLine: CommandLine): Resolver {
    val values = commandLine.getOptionValues("cp") ?: return Resolver.getEmptyResolver()

    val pools = ArrayList<Resolver>(values.size)

    for (value in values) {
      pools.add(Resolver.createJarResolver(File(value)))
    }

    return Resolver.createUnionResolver("External classpath resolver: " + Arrays.toString(values), pools)
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
        throw IllegalArgumentException("The file $file doesn't exist")
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

object VOptionsUtil {

  @JvmStatic
  fun parseOpts(vararg cmd: String): VOptions = parseOpts(GnuParser().parse(Util.CMD_OPTIONS, cmd))

  @JvmStatic
  fun parseOpts(commandLine: CommandLine): VOptions {
    val prefixesToSkipForDuplicateClassesCheck = getOptionValuesSplit(commandLine, ":", "s")
    for (i in prefixesToSkipForDuplicateClassesCheck.indices) {
      prefixesToSkipForDuplicateClassesCheck[i] = prefixesToSkipForDuplicateClassesCheck[i].replace('.', '/')
    }

    val externalClasses = getOptionValuesSplit(commandLine, ":", "e")
    for (i in externalClasses.indices) {
      externalClasses[i] = externalClasses[i].replace('.', '/')
    }
    val optionalDependenciesIdsToIgnoreIfMissing: Set<String> = HashSet(getOptionValuesSplit(commandLine, ",", "imod").toList())

    var problemsToIgnore: Multimap<Pair<String, String>, Pattern> = HashMultimap.create<Pair<String, String>, Pattern>()

    val ignoreProblemsFile = getOption(commandLine, "ip")
    if (ignoreProblemsFile != null) {
      problemsToIgnore = getProblemsToIgnoreFromFile(ignoreProblemsFile)
    }

    return VOptions(prefixesToSkipForDuplicateClassesCheck, externalClasses, optionalDependenciesIdsToIgnoreIfMissing, problemsToIgnore)
  }

  private fun getOption(commandLine: CommandLine, shortKey: String): String? {
    val option = Util.CMD_OPTIONS.getOption(shortKey)

    val cmdValue = commandLine.getOptionValue(shortKey)
    if (cmdValue != null) return cmdValue

    return RepositoryConfiguration.getInstance().getProperty(option.longOpt)
  }

  private fun getOptionValues(commandLine: CommandLine, shortKey: String): List<String> {
    val res = ArrayList<String>()

    val cmdValues = commandLine.getOptionValues(shortKey)
    if (cmdValues != null) {
      Collections.addAll(res, *cmdValues)
    }

    val option = Util.CMD_OPTIONS.getOption(shortKey)
    val cfgProperty = RepositoryConfiguration.getInstance().getProperty(option.longOpt)

    if (cfgProperty != null) {
      res.add(cfgProperty)
    }

    return res
  }

  private fun getOptionValuesSplit(commandLine: CommandLine, splitter: String, shortKey: String): Array<String> {
    val res = ArrayList<String>()
    for (optionStr in getOptionValues(commandLine, shortKey)) {
      if (optionStr.isEmpty()) continue

      Collections.addAll(res, *optionStr.split(splitter.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
    }

    return res.toTypedArray()
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
