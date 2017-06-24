package com.jetbrains.pluginverifier.configurations

import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.api.JdkDescriptor
import com.jetbrains.pluginverifier.api.PluginCoordinate
import com.jetbrains.pluginverifier.misc.closeOnException
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.repository.RepositoryManager
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.utils.IdeResourceUtil
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException

class CheckIdeParamsBuilder : TaskParametersBuilder<CheckIdeParams> {
  override fun build(opts: CmdOpts, freeArgs: List<String>): CheckIdeParams {
    if (freeArgs.isEmpty()) {
      System.err.println("You have to specify IDE to check. For example: \"java -jar verifier.jar check-ide ~/EAPs/idea-IU-133.439\"")
      System.exit(1)
    }
    val ideFile = File(freeArgs[0])
    if (!ideFile.isDirectory) {
      System.err.println("IDE path must be a directory: " + ideFile)
      System.exit(1)
    }
    OptionsParser.createIdeDescriptor(ideFile, opts).closeOnException { ideDescriptor ->
      val jdkDescriptor = JdkDescriptor(OptionsParser.getJdkDir(opts))
      val externalClassesPrefixes = OptionsParser.getExternalClassesPrefixes(opts)
      OptionsParser.getExternalClassPath(opts).closeOnException { externalClassPath ->
        val problemsFilter = OptionsParser.getProblemsFilter(opts)

        val (checkAllBuilds, checkLastBuilds) = parsePluginToCheckList(opts)

        val excludedPlugins = parseExcludedPlugins(opts)

        val pluginsToCheck = getDescriptorsToCheck(checkAllBuilds, checkLastBuilds, ideDescriptor.ideVersion)
        return CheckIdeParams(ideDescriptor, jdkDescriptor, pluginsToCheck, excludedPlugins, externalClassesPrefixes, externalClassPath, checkAllBuilds, problemsFilter)
      }
    }
  }

  /**
   * (id-s of plugins to check all builds, id-s of plugins to check last builds)
   */
  private fun parsePluginToCheckList(opts: CmdOpts): Pair<List<String>, List<String>> {
    val pluginsCheckAllBuilds = arrayListOf<String>()
    val pluginsCheckLastBuilds = arrayListOf<String>()

    pluginsCheckAllBuilds.addAll(opts.pluginToCheckAllBuilds)
    pluginsCheckLastBuilds.addAll(opts.pluginToCheckLastBuild)

    val pluginsFile = opts.pluginsToCheckFile
    if (pluginsFile != null) {
      try {
        BufferedReader(FileReader(pluginsFile)).use { reader ->
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
        }
      } catch (e: IOException) {
        throw RuntimeException("Failed to read plugins file " + pluginsFile + ": " + e.message, e)
      }

    }

    return Pair<List<String>, List<String>>(pluginsCheckAllBuilds, pluginsCheckLastBuilds)
  }


  private fun getDescriptorsToCheck(checkAllBuilds: List<String>, checkLastBuilds: List<String>, ideVersion: IdeVersion): List<PluginCoordinate> =
      getUpdateInfosToCheck(checkAllBuilds, checkLastBuilds, ideVersion).map { PluginCoordinate.ByUpdateInfo(it) }

  private fun getUpdateInfosToCheck(checkAllBuilds: List<String>, checkLastBuilds: List<String>, ideVersion: IdeVersion): List<UpdateInfo> {
    if (checkAllBuilds.isEmpty() && checkLastBuilds.isEmpty()) {
      return RepositoryManager.getLastCompatibleUpdates(ideVersion)
    } else {
      val myActualUpdatesToCheck = arrayListOf<UpdateInfo>()

      checkAllBuilds.flatMapTo(myActualUpdatesToCheck) {
        RepositoryManager.getAllCompatibleUpdatesOfPlugin(ideVersion, it)
      }

      checkLastBuilds.distinct().mapNotNullTo(myActualUpdatesToCheck) {
        RepositoryManager.getAllCompatibleUpdatesOfPlugin(ideVersion, it)
            .sortedByDescending { it.updateId }
            .firstOrNull()
      }

      return myActualUpdatesToCheck
    }
  }

  private fun parseExcludedPlugins(opts: CmdOpts): List<PluginIdAndVersion> {
    val epf = opts.excludedPluginsFile ?: return emptyList()
    File(epf).bufferedReader().use { br ->
      return IdeResourceUtil.getBrokenPluginsByLines(br.readLines())
    }
  }


}