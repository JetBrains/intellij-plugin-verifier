package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.JdkDescriptor
import com.jetbrains.pluginverifier.api.ProblemsFilter
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.misc.deleteLogged
import java.io.File


data class CheckTrunkApiParams(val trunkIde: IdeDescriptor,
                               val releaseIde: IdeDescriptor,
                               val externalClassesPrefixes: List<String>,
                               val problemsFilter: ProblemsFilter,
                               val jdkDescriptor: JdkDescriptor,
                               val jetBrainsPluginIds: List<String>,
                               private val deleteMajorIdeOnExit: Boolean,
                               private val majorIdeFile: File) : TaskParameters {
  override fun presentableText(): String = """Check Trunk API Configuration Parameters:
Trunk IDE to be checked: $trunkIde
Release IDE to compare API with: $releaseIde
External classes prefixes: [${externalClassesPrefixes.joinToString()}]
JDK: $jdkDescriptor
"""

  override fun close() {
    trunkIde.closeLogged()
    releaseIde.closeLogged()
    if (deleteMajorIdeOnExit) {
      majorIdeFile.deleteLogged()
    }
  }

  override fun toString(): String = presentableText()
}