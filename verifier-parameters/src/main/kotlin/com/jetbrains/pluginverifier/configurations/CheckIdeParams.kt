package com.jetbrains.pluginverifier.configurations

import com.intellij.structure.ide.IdeVersion
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.dependency.DependencyResolver
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.misc.closeOnException
import com.jetbrains.pluginverifier.repository.RepositoryManager
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.utils.CmdOpts
import com.jetbrains.pluginverifier.utils.IdeResourceUtil
import com.jetbrains.pluginverifier.utils.OptionsUtil
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException


data class CheckIdeParams(val ideDescriptor: IdeDescriptor,
                          val jdkDescriptor: JdkDescriptor,
                          val pluginsToCheck: List<PluginDescriptor>,
                          val excludedPlugins: List<PluginIdAndVersion>,
                          val pluginIdsToCheckExistingBuilds: List<String>,
                          val externalClassPath: Resolver,
                          val externalClassesPrefixes: List<String>,
                          val problemsFilter: ProblemsFilter,
                          val progress: Progress = DefaultProgress(),
                          val dependencyResolver: DependencyResolver? = null) : ConfigurationParams {
  override fun presentableText(): String = """Check IDE configuration parameters:
IDE to be checked: $ideDescriptor
JDK: $jdkDescriptor
Plugins to be checked: [${pluginsToCheck.joinToString()}]
Excluded plugins: [${excludedPlugins.joinToString()}]
"""

  override fun close() {
    ideDescriptor.closeLogged()
    pluginsToCheck.forEach { it.closeLogged() }
    externalClassPath.closeLogged()
  }

  override fun toString(): String = presentableText()
}


class CheckIdeParamsParser : ConfigurationParamsParser<CheckIdeParams> {
  override fun parse(opts: CmdOpts, freeArgs: List<String>): CheckIdeParams {
    if (freeArgs.isEmpty()) {
      System.err.println("You have to specify IDE to check. For example: \"java -jar verifier.jar check-ide ~/EAPs/idea-IU-133.439\"")
      System.exit(1)
    }
    val ideFile = File(freeArgs[0])
    if (!ideFile.isDirectory) {
      System.err.println("IDE path must be a directory: " + ideFile)
      System.exit(1)
    }
    OptionsUtil.createIdeDescriptor(ideFile, opts).closeOnException { ideDescriptor ->
      val jdkDescriptor = JdkDescriptor(OptionsUtil.getJdkDir(opts))
      val externalClassesPrefixes = OptionsUtil.getExternalClassesPrefixes(opts)
      OptionsUtil.getExternalClassPath(opts).closeOnException { externalClassPath ->
        val problemsFilter = OptionsUtil.getProblemsFilter(opts)

        val (checkAllBuilds, checkLastBuilds) = parsePluginToCheckList(opts)

        val excludedPlugins = parseExcludedPlugins(opts)

        getDescriptorsToCheck(checkAllBuilds, checkLastBuilds, ideDescriptor.ideVersion).closeOnException { pluginsToCheck ->
          return CheckIdeParams(ideDescriptor, jdkDescriptor, pluginsToCheck, excludedPlugins, externalClassesPrefixes, externalClassPath, checkAllBuilds, problemsFilter)
        }
      }
    }
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


  fun getDescriptorsToCheck(checkAllBuilds: List<String>, checkLastBuilds: List<String>, ideVersion: IdeVersion): List<PluginDescriptor> =
      getUpdateInfosToCheck(checkAllBuilds, checkLastBuilds, ideVersion).map { PluginDescriptor.ByUpdateInfo(it) }

  private fun getUpdateInfosToCheck(checkAllBuilds: List<String>, checkLastBuilds: List<String>, ideVersion: IdeVersion): List<UpdateInfo> {
    if (checkAllBuilds.isEmpty() && checkLastBuilds.isEmpty()) {
      return RepositoryManager.getLastCompatibleUpdates(ideVersion)
    } else {
      val myActualUpdatesToCheck = arrayListOf<UpdateInfo>()

      checkAllBuilds.map {
        RepositoryManager.getAllCompatibleUpdatesOfPlugin(ideVersion, it)
      }.flatten().toCollection(myActualUpdatesToCheck)

      checkLastBuilds.distinct().map {
        RepositoryManager.getAllCompatibleUpdatesOfPlugin(ideVersion, it)
            .sortedByDescending { it.updateId }
            .firstOrNull()
      }.filterNotNull().toCollection(myActualUpdatesToCheck)

      return myActualUpdatesToCheck
    }
  }

  fun parseExcludedPlugins(opts: CmdOpts): List<PluginIdAndVersion> {
    val epf = opts.excludedPluginsFile ?: return emptyList()
    File(epf).bufferedReader().use { br ->
      return IdeResourceUtil.getBrokenPluginsByLines(br.readLines())
    }
  }


}
