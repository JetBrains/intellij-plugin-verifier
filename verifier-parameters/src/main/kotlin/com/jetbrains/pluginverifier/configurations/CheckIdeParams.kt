package com.jetbrains.pluginverifier.configurations

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.intellij.structure.domain.IdeVersion
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.repository.RepositoryManager
import com.jetbrains.pluginverifier.utils.CmdOpts
import com.jetbrains.pluginverifier.utils.CmdUtil
import com.jetbrains.pluginverifier.utils.ParametersListUtil
import com.jetbrains.pluginverifier.utils.VOptionsUtil
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException


data class CheckIdeParams(val ideDescriptor: IdeDescriptor,
                          val jdkDescriptor: JdkDescriptor,
                          val pluginsToCheck: List<PluginDescriptor>,
                          val excludedPlugins: Multimap<String, String>,
                          val vOptions: VOptions,
                          val externalClassPath: Resolver = Resolver.getEmptyResolver(),
                          val progress: VProgress = DefaultVProgress()) : Params


object CheckIdeParamsParser : ParamsParser {
  override fun parse(opts: CmdOpts, freeArgs: List<String>): CheckIdeParams {
    if (freeArgs.isEmpty()) {
      throw RuntimeException("You have to specify IDE to check. For example: \"java -jar verifier.jar check-ide ~/EAPs/idea-IU-133.439\"")
    }
    val ideFile = File(freeArgs[0])
    if (!ideFile.isDirectory) {
      throw RuntimeException("IDE home is not a directory: " + ideFile)
    }
    val ide = CmdUtil.createIde(ideFile, opts)

    val jdkDescriptor = JdkDescriptor.ByFile(CmdUtil.getJdkDir(opts))
    val vOptions = VOptionsUtil.parseOpts(opts)
    val externalClassPath = CmdUtil.getExternalClassPath(opts)

    val (checkAllBuilds, checkLastBuilds) = parsePluginToCheckList(opts)

    val excludedPlugins = parseExcludedPlugins(opts)

    val pluginsToCheck = getDescriptorsToCheck(checkAllBuilds, checkLastBuilds, ide.version)

    return CheckIdeParams(IdeDescriptor.ByInstance(ide), jdkDescriptor, pluginsToCheck, excludedPlugins, vOptions, externalClassPath)
  }

  /**
   * (id-s of plugins to check all builds, id-s of plugins to check last builds)
   */
  fun parsePluginToCheckList(opts: CmdOpts): Pair<List<String>, List<String>> {
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

          if (s.isEmpty()) continue

          if (checkAllBuilds) {
            pluginsCheckAllBuilds.add(s)
          } else {
            pluginsCheckLastBuilds.add(s)
          }
        }
      } catch (e: IOException) {
        throw RuntimeException("Failed to read plugins file " + pluginsFile + ": " + e.message, e)
      }

    }

    return Pair<List<String>, List<String>>(pluginsCheckAllBuilds, pluginsCheckLastBuilds)
  }


  fun getDescriptorsToCheck(checkAllBuilds: List<String>, checkLastBuilds: List<String>, ideVersion: IdeVersion): List<PluginDescriptor> {
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
  fun parseExcludedPlugins(opts: CmdOpts): Multimap<String, String> {
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
