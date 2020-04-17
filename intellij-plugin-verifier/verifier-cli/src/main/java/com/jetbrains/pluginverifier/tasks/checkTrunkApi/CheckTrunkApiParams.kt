/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

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
    get() = buildString {
      appendln("Trunk IDE: $trunkIde")
      appendln("Release IDE: $releaseIde")

      appendln("Release verifications (${releaseVerificationDescriptors.size}): ")
      for ((ideVersion, ideVerifications) in releaseVerificationDescriptors.groupBy { it.ideVersion }) {
        appendln(ideVersion.asString() + " against " + ideVerifications.joinToString { it.checkedPlugin.presentableName })
      }

      appendln("Trunk verifications (${releaseVerificationDescriptors.size}): ")
      for ((ideVersion, ideVerifications) in trunkVerificationDescriptors.groupBy { it.ideVersion }) {
        appendln(ideVersion.asString() + " against " + ideVerifications.joinToString { it.checkedPlugin.presentableName })
      }
    }

  override fun close() {
    trunkIde.closeLogged()
    releaseIde.closeLogged()
    releaseIdeFile.release()
    if (deleteReleaseIdeOnExit) {
      releaseIdeFile.file.deleteLogged()
    }
  }

}