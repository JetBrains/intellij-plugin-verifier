package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.tasks.InvalidPluginFile
import com.jetbrains.pluginverifier.tasks.TaskParameters

class CheckPluginParams(
    private val ideDescriptors: List<IdeDescriptor>,
    val problemsFilters: List<ProblemsFilter>,
    val verificationDescriptors: List<PluginVerificationDescriptor>,
    val invalidPluginFiles: List<InvalidPluginFile>
) : TaskParameters {

  override val presentableText
    get() = buildString {
      appendln("Scheduled verifications (${verificationDescriptors.size}):")
      appendln(verificationDescriptors.joinToString())
    }

  override fun close() {
    ideDescriptors.forEach { it.closeLogged() }
  }

}