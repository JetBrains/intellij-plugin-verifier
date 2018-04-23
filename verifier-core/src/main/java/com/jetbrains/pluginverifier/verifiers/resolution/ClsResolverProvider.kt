package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.ResultHolder
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.reporting.verification.PluginVerificationReportage

interface ClsResolverProvider {

  fun create(pluginDetails: PluginDetails,
             resultHolder: ResultHolder,
             reportage: PluginVerificationReportage): ClsResolver

}