package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.Task
import com.jetbrains.pluginverifier.tasks.checkIde.CheckIdeTask

class DeprecatedUsagesTask(private val parameters: DeprecatedUsagesParams,
                           val pluginRepository: PluginRepository,
                           val pluginDetailsProvider: PluginDetailsProvider) : Task() {

  override fun execute(verificationReportage: VerificationReportage): DeprecatedUsagesResult {
    val checkIdeTask = CheckIdeTask(parameters.checkIdeParams, pluginRepository, pluginDetailsProvider)
    val checkIdeResult = checkIdeTask.execute(verificationReportage)
    return DeprecatedUsagesResult(checkIdeResult.ideVersion, checkIdeResult.results)
  }

}

