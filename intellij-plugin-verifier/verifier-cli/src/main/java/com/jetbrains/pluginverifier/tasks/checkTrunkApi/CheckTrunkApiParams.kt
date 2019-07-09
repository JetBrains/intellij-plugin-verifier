package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.base.utils.deleteLogged
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.options.PluginsSet
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.verifiers.packages.PackageFilter
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.files.FileLock
import com.jetbrains.pluginverifier.tasks.TaskParameters
import java.nio.file.Path


class CheckTrunkApiParams(
    val releasePluginsSet: PluginsSet,
    val trunkPluginsSet: PluginsSet,
    val jdkPath: Path,
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