package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.TaskParametersBuilder
import com.jetbrains.pluginverifier.tasks.checkIde.CheckIdeParamsBuilder
import java.io.File

class DeprecatedUsagesParamsBuilder(val pluginRepository: PluginRepository, val pluginDetailsProvider: PluginDetailsProvider) : TaskParametersBuilder {
  override fun build(opts: CmdOpts, freeArgs: List<String>): DeprecatedUsagesParams {
    if (freeArgs.isEmpty()) {
      throw IllegalArgumentException("You have to specify IDE to detect deprecated changes of. For example: \"java -jar verifier.jar check-ide ~/EAPs/idea-IU-133.439\"")
    }
    val ideFile = File(freeArgs[0])
    if (!ideFile.isDirectory) {
      throw IllegalArgumentException("IDE path must be a directory: " + ideFile)
    }
    val checkIdeParams = CheckIdeParamsBuilder(pluginRepository, pluginDetailsProvider).build(opts, freeArgs)
    return DeprecatedUsagesParams(checkIdeParams)
  }

}