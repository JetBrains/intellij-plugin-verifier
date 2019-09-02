package com.jetbrains.pluginverifier.tasks.checkPluginApi

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.jdk.JdkDescriptor
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.tasks.TaskParameters

class CheckPluginApiParams(
    private val basePluginDetails: PluginDetails,
    private val newPluginDetails: PluginDetails,
    private val jdkDescriptor: JdkDescriptor,
    val problemsFilters: List<ProblemsFilter>,
    val baseVerificationDescriptors: List<PluginVerificationDescriptor.Plugin>,
    val newVerificationDescriptors: List<PluginVerificationDescriptor.Plugin>,
    val baseVerificationTarget: PluginVerificationTarget.Plugin,
    val newVerificationTarget: PluginVerificationTarget.Plugin
) : TaskParameters {

  override val presentableText
    get() = """
      |${baseVerificationDescriptors.joinToString()}
      |${newVerificationDescriptors.joinToString()}
    """.trimMargin()

  override fun close() {
    basePluginDetails.closeLogged()
    newPluginDetails.closeLogged()
    jdkDescriptor.closeLogged()
  }

}