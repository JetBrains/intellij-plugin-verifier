package com.jetbrains.pluginverifier.configurations

import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.misc.deleteLogged
import java.io.File


data class CheckTrunkApiParams(val trunkDescriptor: IdeDescriptor,
                               val releaseDescriptor: IdeDescriptor,
                               val externalClassesPrefixes: List<String>,
                               val problemsFilter: ProblemsFilter,
                               val jdkDescriptor: JdkDescriptor,
                               private val deleteMajorIdeOnExit: Boolean,
                               private val majorIdeFile: File,
                               val progress: Progress = DefaultProgress()) : TaskParameters {
  override fun presentableText(): String = """Check Trunk API Configuration Parameters:
Trunk IDE to be checked: $trunkDescriptor
Release IDE to compare API with: $releaseDescriptor
External classes prefixes: [${externalClassesPrefixes.joinToString()}]
JDK: $jdkDescriptor
"""

  override fun close() {
    trunkDescriptor.closeLogged()
    releaseDescriptor.closeLogged()
    if (deleteMajorIdeOnExit) {
      majorIdeFile.deleteLogged()
    }
  }

  override fun toString(): String = presentableText()
}