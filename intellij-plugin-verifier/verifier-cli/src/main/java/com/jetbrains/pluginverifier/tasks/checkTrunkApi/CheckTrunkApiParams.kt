package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.base.utils.deleteLogged
import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.repository.files.FileLock
import com.jetbrains.pluginverifier.tasks.TaskParameters


class CheckTrunkApiParams(
    private val trunkIde: IdeDescriptor,
    private val releaseIde: IdeDescriptor,
    private val deleteReleaseIdeOnExit: Boolean,
    private val releaseIdeFile: FileLock,
    val problemsFilters: List<ProblemsFilter>,
    val releaseVerificationDescriptors: List<PluginVerificationDescriptor.IDE>,
    val trunkVerificationDescriptors: List<PluginVerificationDescriptor.IDE>,
    val releaseVerificationTarget: PluginVerificationTarget.IDE,
    val trunkVerificationTarget: PluginVerificationTarget.IDE
) : TaskParameters {
  override val presentableText: String
    get() = """
      |Trunk IDE        : $trunkIde
      |Release IDE      : $releaseIde
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