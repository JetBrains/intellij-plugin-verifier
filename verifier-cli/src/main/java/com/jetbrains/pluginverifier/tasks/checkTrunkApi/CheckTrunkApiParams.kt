package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import com.jetbrains.pluginverifier.repository.files.FileLock
import com.jetbrains.pluginverifier.repository.local.LocalPluginRepository
import com.jetbrains.pluginverifier.tasks.PluginsToCheck
import com.jetbrains.pluginverifier.tasks.TaskParameters


class CheckTrunkApiParams(pluginsToCheck: PluginsToCheck,
                          val jdkPath: JdkPath,
                          val trunkIde: IdeDescriptor,
                          val releaseIde: IdeDescriptor,
                          val externalClassesPrefixes: List<String>,
                          val problemsFilters: List<ProblemsFilter>,
                          val jdkDescriptorsCache: JdkDescriptorsCache,
                          val jetBrainsPluginIds: List<String>,
                          private val deleteReleaseIdeOnExit: Boolean,
                          private val releaseIdeFile: FileLock,
                          val releaseLocalPluginsRepository: LocalPluginRepository?,
                          val trunkLocalPluginsRepository: LocalPluginRepository?) : TaskParameters(pluginsToCheck) {
  override fun presentableText(): String = """Check Trunk API Configuration Parameters:
Trunk IDE to be checked: $trunkIde
Release IDE to compare API with: $releaseIde
External classes prefixes: [${externalClassesPrefixes.joinToString()}]
JDK: $jdkPath
"""

  override fun close() {
    trunkIde.closeLogged()
    releaseIde.closeLogged()
    releaseIdeFile.release()
    if (deleteReleaseIdeOnExit) {
      releaseIdeFile.file.deleteLogged()
    }
    jdkDescriptorsCache.closeLogged()
  }

  override fun toString(): String = presentableText()
}