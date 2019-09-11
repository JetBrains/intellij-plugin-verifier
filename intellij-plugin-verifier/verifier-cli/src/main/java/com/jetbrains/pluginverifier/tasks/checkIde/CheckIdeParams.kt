package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.tasks.TaskParameters


class CheckIdeParams(
    val verificationTarget: PluginVerificationTarget.IDE,
    val verificationDescriptors: List<PluginVerificationDescriptor.IDE>,
    val problemsFilters: List<ProblemsFilter>,
    val missingCompatibleVersionsProblems: List<MissingCompatibleVersionProblem>,
    private val ideDescriptor: IdeDescriptor
) : TaskParameters {

  override val presentableText
    get() = buildString {
      appendln("Verifications (${verificationDescriptors.size}:")
      for ((ideVersion, ideVerifications) in verificationDescriptors.groupBy { it.ideVersion }) {
        appendln(ideVersion.asString() + " against " + ideVerifications.joinToString { it.checkedPlugin.presentableName })
      }
    }

  override fun close() {
    ideDescriptor.closeLogged()
  }

}