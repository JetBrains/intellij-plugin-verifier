package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.options.PluginsSet
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import com.jetbrains.pluginverifier.parameters.packages.PackageFilter
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.files.FileLock
import com.jetbrains.pluginverifier.tasks.TaskParameters


class CheckTrunkApiParams(
    val pluginsSet: PluginsSet,
    val jdkPath: JdkPath,
    val trunkIde: IdeDescriptor,
    val releaseIde: IdeDescriptor,
    val externalClassesPackageFilter: PackageFilter,
    val problemsFilters: List<ProblemsFilter>,
    private val deleteReleaseIdeOnExit: Boolean,
    private val releaseIdeFile: FileLock,
    val releaseLocalPluginsRepository: PluginRepository,
    val trunkLocalPluginsRepository: PluginRepository
) : TaskParameters {
  override val presentableText: String
    get() = """
      |Trunk IDE        : $trunkIde
      |Release IDE      : $releaseIde
      |JDK              : $jdkPath
    """.trimMargin()

  override fun close() {
    trunkIde.closeLogged()
    releaseIde.closeLogged()
    releaseIdeFile.release()
    if (deleteReleaseIdeOnExit) {
      releaseIdeFile.file.deleteLogged()
    }
  }

}