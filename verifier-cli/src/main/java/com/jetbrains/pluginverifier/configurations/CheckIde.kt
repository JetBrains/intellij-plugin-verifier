package com.jetbrains.pluginverifier.configurations

import com.google.common.base.Joiner
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.HashMultimap
import com.google.common.collect.Iterables
import com.google.common.collect.Multimap
import com.intellij.structure.domain.IdeVersion
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.output.HtmlVPrinter
import com.jetbrains.pluginverifier.output.StreamVPrinter
import com.jetbrains.pluginverifier.output.TeamCityLog
import com.jetbrains.pluginverifier.output.TeamCityVPrinter
import com.jetbrains.pluginverifier.problems.NoCompatibleUpdatesProblem
import com.jetbrains.pluginverifier.repository.RepositoryManager
import com.jetbrains.pluginverifier.utils.*
import java.io.*

object CheckIdeParamsParser : ParamsParser {
  override fun parse(opts: Opts, freeArgs: List<String>): CheckIdeParams {
    if (freeArgs.isEmpty()) {
      throw RuntimeException("You have to specify IDE to check. For example: \"java -jar verifier.jar check-ide ~/EAPs/idea-IU-133.439\"")
    }
    val ideFile = File(freeArgs[0])
    if (!ideFile.isDirectory) {
      throw RuntimeException("IDE home is not a directory: " + ideFile)
    }
    val ide = Util.createIde(ideFile, opts)

    val jdkDescriptor = JdkDescriptor.ByFile(Util.getJdkDir(opts))
    val vOptions = VOptionsUtil.parseOpts(opts)
    val externalClassPath = Util.getExternalClassPath(opts)

    val (checkAllBuilds, checkLastBuilds) = extractPluginToCheckList(opts)

    val excludedPlugins = getExcludedPlugins(opts)

    val pluginsToCheck = getDescriptorsToCheck(checkAllBuilds, checkLastBuilds, ide.version)

    return CheckIdeParams(IdeDescriptor.ByInstance(ide), jdkDescriptor, pluginsToCheck, excludedPlugins, vOptions, externalClassPath)
  }

  /**
   * (id-s of plugins to check all builds, id-s of plugins to check last builds)
   */
  private fun extractPluginToCheckList(opts: Opts): Pair<List<String>, List<String>> {
    val pluginsCheckAllBuilds = arrayListOf<String>()
    val pluginsCheckLastBuilds = arrayListOf<String>()

    pluginsCheckAllBuilds.addAll(opts.pluginToCheckAllBuilds)
    pluginsCheckLastBuilds.addAll(opts.pluginToCheckLastBuild)

    val pluginsFile = opts.pluginsToCheckFile
    if (pluginsFile != null) {
      try {
        val reader = BufferedReader(FileReader(pluginsFile))
        var s: String?
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


  private fun getDescriptorsToCheck(checkAllBuilds: List<String>, checkLastBuilds: List<String>, ideVersion: IdeVersion): List<PluginDescriptor> {
    if (checkAllBuilds.isEmpty() && checkLastBuilds.isEmpty()) {
      return RepositoryManager.getInstance().getLastCompatibleUpdates(ideVersion).map { PluginDescriptor.ByUpdateInfo(it.pluginId ?: "", it.version ?: "", it) }
    } else {
      val myActualUpdatesToCheck = arrayListOf<UpdateInfo>()

      checkAllBuilds.map {
        RepositoryManager.getInstance().getAllCompatibleUpdatesOfPlugin(ideVersion, it)
      }.flatten().toCollection(myActualUpdatesToCheck)

      checkLastBuilds.distinct().map {
        RepositoryManager.getInstance()
            .getAllCompatibleUpdatesOfPlugin(ideVersion, it)
            .filter { it.updateId != null }
            .sortedByDescending { it.updateId }
            .firstOrNull()
      }.filterNotNull().toCollection(myActualUpdatesToCheck)

      return myActualUpdatesToCheck.map { PluginDescriptor.ByUpdateInfo(it.pluginId ?: "", it.version ?: "", it) }
    }
  }

  /**
   * Plugin Id -> Versions
   */
  @Throws(IOException::class)
  private fun getExcludedPlugins(opts: Opts): Multimap<String, String> {
    val epf = opts.excludedPluginsFile ?: return ArrayListMultimap.create<String, String>() //excluded-plugin-file (usually brokenPlugins.txt)

    //file containing list of broken plugins (e.g. IDEA-*/lib/resources.jar!/brokenPlugins.txt)
    BufferedReader(FileReader(File(epf))).use { br ->
      val m = HashMultimap.create<String, String>()

      var s: String?
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


}

class CheckIdeParams(val ideDescriptor: IdeDescriptor,
                     val jdkDescriptor: JdkDescriptor,
                     val pluginsToCheck: List<PluginDescriptor>,
                     val excludedPlugins: Multimap<String, String>,
                     val vOptions: VOptions,
                     val externalClassPath: Resolver) : Params {

}

class CheckIdeResults(val ideVersion: IdeVersion,
                      val vResults: VResults,
                      val params: CheckIdeParams,
                      val noCompatibleUpdatesProblems: List<NoCompatibleUpdatesProblem>) : Results {

  fun dumbBrokenPluginsList(dumpBrokenPluginsFile: File) {
    PrintWriter(dumpBrokenPluginsFile.apply { parentFile.mkdirs() }).use { out ->
      out.println("// This file contains list of broken plugins.\n" +
          "// Each line contains plugin ID and list of versions that are broken.\n" +
          "// If plugin name or version contains a space you can quote it like in command line.\n")

      val brokenPlugins = vResults.results.filterNot { it is VResult.Nice }.map { it.pluginDescriptor }.map { it.pluginId to it.version }.distinct()
      brokenPlugins.groupBy { it.first }.forEach {
        out.print(ParametersListUtil.join(listOf(it.key)))
        out.print("    ")
        out.println(ParametersListUtil.join(it.value.map { it.second }.sortedWith(VersionComparatorUtil.COMPARATOR)))
      }
    }
  }

  fun saveResultsToFile(file: File) {
    PrintStream(file).use { StreamVPrinter(it).printResults(vResults) }
  }

  fun saveToHtmlFile(htmlFile: File) {
    HtmlVPrinter(ideVersion, { x -> params.excludedPlugins.containsEntry(x.first, x.second) }, htmlFile).printResults(vResults)
  }

  fun processResults(opts: Opts) {
    if (opts.needTeamCityLog) {
      val vPrinter = TeamCityVPrinter(TeamCityLog(System.out), TeamCityVPrinter.GroupBy.parse(opts))
      vPrinter.printResults(vResults)
      vPrinter.printNoCompatibleUpdatesProblems(noCompatibleUpdatesProblems)
      //TODO: set tc-build status to either success or fail
    }
    if (opts.htmlReportFile != null) {
      saveToHtmlFile(File(opts.htmlReportFile))
    }
    if (opts.dumpBrokenPluginsFile != null) {
      dumbBrokenPluginsList(File(opts.dumpBrokenPluginsFile))
    }
    if (opts.resultFile != null) {
      saveResultsToFile(File(opts.resultFile))
    }
  }

}

class CheckIdeConfiguration(val params: CheckIdeParams) : Configuration {
  override fun execute(): CheckIdeResults {
    val pluginsToCheck = params.pluginsToCheck.filterNot { params.excludedPlugins.containsEntry(it.pluginId, it.version) }.map { it to params.ideDescriptor }
    val vParams = VParams(params.jdkDescriptor, pluginsToCheck, params.vOptions, params.externalClassPath)
    val vResults = VManager.verify(vParams)
    return CheckIdeResults(params.ideDescriptor.ideVersion, vResults, params, getMissingUpdatesProblems())
  }

  private fun getMissingUpdatesProblems(): List<NoCompatibleUpdatesProblem> {
    val ideVersion = params.ideDescriptor.ideVersion
    val existingUpdatesForIde = RepositoryManager.getInstance()
        .getLastCompatibleUpdates(ideVersion)
        .filterNot { params.excludedPlugins.containsEntry(it.pluginId, it.version) }
        .map { it.pluginId }
        .filterNotNull()
        .toSet()

    return params.pluginsToCheck.map { it.pluginId }.distinct()
        .filterNot { existingUpdatesForIde.contains(it) }
        .map {
          val buildForCommunity = getUpdateCompatibleWithCommunityEdition(it, ideVersion)
          if (buildForCommunity != null) {
            val details = "\nNote: there is an update (#" + buildForCommunity.updateId + ") compatible with IDEA Community Edition, " +
                "\nbut the Plugin repository does not offer to install it if you run the IDEA Ultimate."
            NoCompatibleUpdatesProblem(it, ideVersion.asString(), details)
          } else {
            NoCompatibleUpdatesProblem(it, ideVersion.asString(), "")
          }
        }
  }

  private fun getUpdateCompatibleWithCommunityEdition(pluginId: String, version: IdeVersion): UpdateInfo? {
    val ideVersion = version.asString()
    if (ideVersion.startsWith("IU-")) {
      val communityVersion = "IC-" + StringUtil.trimStart(ideVersion, "IU-")
      try {
        return RepositoryManager.getInstance().getLastCompatibleUpdateOfPlugin(IdeVersion.createIdeVersion(communityVersion), pluginId)
      } catch (e: Exception) {
        return null
      }

    }
    return null
  }


}