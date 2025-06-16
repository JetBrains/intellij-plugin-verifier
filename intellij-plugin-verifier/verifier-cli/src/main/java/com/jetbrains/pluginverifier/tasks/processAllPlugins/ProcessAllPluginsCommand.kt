package com.jetbrains.pluginverifier.tasks.processAllPlugins

import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.intellij.plugin.caches.PluginResourceCache
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.misc.retry
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.repositories.local.LocalPluginRepositoryFactory
import com.jetbrains.pluginverifier.tasks.CommandRunner
import com.jetbrains.pluginverifier.tasks.TaskParameters
import com.jetbrains.pluginverifier.tasks.TaskParametersBuilder
import java.nio.file.Path
import java.nio.file.Paths

/**
 * `processAllPlugins` command allows to run some processing of all plugins from JetBrains Marketplace.
 *
 * `countUsagesOfExtensionPoints` goal counts usages of IDE extension points inside all JetBrains Marketplace plugins and saves it as a .json file
 *
 * ```java -jar verifier.jar processAllPlugins countUsagesOfExtensionPoints <IDE path> <IDE plugins path> <output.json>```
 */
class ProcessAllPluginsCommand : CommandRunner {
  override val commandName: String
    get() = "processAllPlugins"

  override fun getParametersBuilder(
    pluginRepository: PluginRepository,
    pluginDetailsCache: PluginDetailsCache,
    extractedPluginCache: PluginResourceCache,
    reportage: PluginVerificationReportage
  ): TaskParametersBuilder = object : TaskParametersBuilder {
    override fun build(opts: CmdOpts, freeArgs: List<String>): TaskParameters {
      require(freeArgs.isNotEmpty()) { "Usage: java -jar verifier.jar processAllPlugins [goal name] <goal parameters (optional)>" }
      return when (val goalName = freeArgs[0]) {
        "countUsagesOfExtensionPoints" -> parseCountUsagesOfExtensionPointsParameters(freeArgs, reportage, opts, pluginRepository)
        else -> error("Unknown goal '$goalName'")
      }
    }
  }

  private fun parseCountUsagesOfExtensionPointsParameters(
    freeArgs: List<String>,
    reportage: PluginVerificationReportage,
    opts: CmdOpts,
    pluginRepository: PluginRepository
  ): TaskParameters {
    require(freeArgs.size > 3) { "'countUsagesOfExtensionPoints' goal requires <ide path> <ide plugins path> <output.json>" }
    val idePath = freeArgs[1]
    val idePluginsRoot = freeArgs[2].let { Paths.get(it) }.also { require(it.exists()) { "Path ${freeArgs[2]} must point to a local root of additional IDE plugins" } }
    val outputJson = freeArgs[3].also { require(it.endsWith(".json")) { "must be <output.json> but was $it" } }.let { Paths.get(it) }

    reportage.logVerificationStage("Reading IDE $idePath")
    OptionsParser.createIdeDescriptor(idePath, opts).closeOnException { ideDescriptor ->
      val compatiblePluginsList = pluginRepository.retry("Request plugins compatible with ${ideDescriptor.ideVersion}") {
        getLastCompatiblePlugins(ideDescriptor.ideVersion)
      }
      val localPluginRepository = LocalPluginRepositoryFactory.createLocalPluginRepository(idePluginsRoot)
      val additionalIdePlugins = localPluginRepository.getLastCompatiblePlugins(ideDescriptor.ideVersion)

      return CountUsagesOfExtensionPointsParameters(ideDescriptor, additionalIdePlugins, compatiblePluginsList, outputJson)
    }
  }
}

class CountUsagesOfExtensionPointsParameters(
  val ideDescriptor: IdeDescriptor,
  val additionalIdePlugins: List<PluginInfo>,
  val compatiblePluginsList: List<PluginInfo>,
  val outputJson: Path
) : TaskParameters {
  override val presentableText
    get() = "Count usages of IDE ${ideDescriptor.ideVersion} extension points inside all compatible plugins available in JetBrains Marketplace"

  override fun createTask() = CountUsagesOfExtensionPointsTask(this)

  override fun close() {
    ideDescriptor.close()
  }
}
