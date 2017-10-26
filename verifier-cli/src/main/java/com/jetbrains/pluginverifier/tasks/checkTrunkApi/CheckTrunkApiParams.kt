package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.parameters.ide.IdeDescriptor
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptor
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.repository.local.LocalPluginRepository
import com.jetbrains.pluginverifier.tasks.TaskParameters
import java.io.File


data class CheckTrunkApiParams(val trunkIde: IdeDescriptor,
                               val releaseIde: IdeDescriptor,
                               val externalClassesPrefixes: List<String>,
                               val problemsFilters: List<ProblemsFilter>,
                               val jdkDescriptor: JdkDescriptor,
                               val jetBrainsPluginIds: List<String>,
                               private val deleteReleaseIdeOnExit: Boolean,
                               private val releaseIdeFile: File,
                               val releaseLocalPluginsRepository: LocalPluginRepository?,
                               val trunkLocalPluginsRepository: LocalPluginRepository?,
                               val pluginsToCheck: List<PluginCoordinate>) : TaskParameters {
  override fun presentableText(): String = """Check Trunk API Configuration Parameters:
Trunk IDE to be checked: $trunkIde
Release IDE to compare API with: $releaseIde
External classes prefixes: [${externalClassesPrefixes.joinToString()}]
JDK: $jdkDescriptor
"""

  override fun close() {
    trunkIde.closeLogged()
    releaseIde.closeLogged()
    if (deleteReleaseIdeOnExit) {
      releaseIdeFile.deleteLogged()
    }
  }

  override fun toString(): String = presentableText()
}