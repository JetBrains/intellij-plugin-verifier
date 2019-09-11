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
    get() = buildString {
      appendln("Base verifications (${baseVerificationDescriptors.size}): ")
      for ((apiPlugin, pluginVerifications) in baseVerificationDescriptors.groupBy { it.apiPlugin }) {
        appendln(apiPlugin.presentableName + " against " + pluginVerifications.joinToString { it.checkedPlugin.presentableName })
      }

      appendln("New verifications: (${newVerificationDescriptors.size}): ")
      for ((apiPlugin, pluginVerifications) in newVerificationDescriptors.groupBy { it.apiPlugin }) {
        appendln(apiPlugin.presentableName + " against " + pluginVerifications.joinToString { it.checkedPlugin.presentableName })
      }
    }

  override fun close() {
    basePluginDetails.closeLogged()
    newPluginDetails.closeLogged()
    jdkDescriptor.closeLogged()
  }

}