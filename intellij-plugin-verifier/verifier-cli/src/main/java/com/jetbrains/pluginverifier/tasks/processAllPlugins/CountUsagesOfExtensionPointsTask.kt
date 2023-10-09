package com.jetbrains.pluginverifier.tasks.processAllPlugins

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.utils.ExecutorWithProgress
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginContentDescriptor
import com.jetbrains.pluginverifier.getConcurrencyLevel
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.Task
import com.jetbrains.pluginverifier.tasks.TaskResult
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class CountUsagesOfExtensionPointsTask(private val params: CountUsagesOfExtensionPointsParameters) : Task {
  private sealed class ProcessingOutcome {
    object Success : ProcessingOutcome()
    data class Failed(val reason: String) : ProcessingOutcome()
  }

  override fun execute(reportage: PluginVerificationReportage, pluginDetailsCache: PluginDetailsCache): TaskResult {
    val ideAndPluginsExtensionPoints = arrayListOf<IdePluginContentDescriptor.ExtensionPoint>()
    for (idePlugin in params.ideDescriptor.ide.bundledPlugins) {
      ideAndPluginsExtensionPoints += idePlugin.appContainerDescriptor.extensionPoints
      ideAndPluginsExtensionPoints += idePlugin.projectContainerDescriptor.extensionPoints
      ideAndPluginsExtensionPoints += idePlugin.moduleContainerDescriptor.extensionPoints
    }
    for (additionalIdePluginInfo in params.additionalIdePlugins) {
      pluginDetailsCache.getPluginDetailsCacheEntry(additionalIdePluginInfo).use { cacheResult ->
        if (cacheResult is PluginDetailsCache.Result.Provided) {
          val idePlugin = cacheResult.pluginDetails.idePlugin
          ideAndPluginsExtensionPoints += idePlugin.appContainerDescriptor.extensionPoints
          ideAndPluginsExtensionPoints += idePlugin.projectContainerDescriptor.extensionPoints
          ideAndPluginsExtensionPoints += idePlugin.moduleContainerDescriptor.extensionPoints
        }
      }
    }

    val extensionPointUsages = ConcurrentHashMap(ideAndPluginsExtensionPoints.map { it.extensionPointName }.associateWith { 0 })
    val tasks = params.compatiblePluginsList.map { plugin ->
      ExecutorWithProgress.Task("$plugin") {
        pluginDetailsCache.getPluginDetailsCacheEntry(plugin).use { cacheResult ->
          when (cacheResult) {
            is PluginDetailsCache.Result.Provided -> {
              for ((extensionPointName, elements) in cacheResult.pluginDetails.idePlugin.extensions) {
                extensionPointUsages.compute(extensionPointName) { _, count ->
                  if (count == null) null /* Count only IDE extension points */ else count + elements.size
                }
              }
              ProcessingOutcome.Success
            }
            is PluginDetailsCache.Result.Failed -> ProcessingOutcome.Failed(cacheResult.reason)
            is PluginDetailsCache.Result.FileNotFound -> ProcessingOutcome.Failed(cacheResult.reason)
            is PluginDetailsCache.Result.InvalidPlugin -> ProcessingOutcome.Failed("Invalid plugin: " + cacheResult.pluginErrors.filter { it.level == PluginProblem.Level.ERROR }.joinToString { it.message })
          }
        }
      }
    }
    val executor = ExecutorWithProgress<ProcessingOutcome>("processAllPlugins [countUsagesOfExtensionPoints]", getConcurrencyLevel(), false) { progressData ->
      val message = buildString {
        append("Finished #${progressData.finishedNumber} of ${progressData.totalNumber}: ")
        if (progressData.exception != null) {
          append("[error] [ ${progressData.exception!!.message} ]")
        } else {
          when (val result = progressData.result!!) {
            is ProcessingOutcome.Success -> append("[success]")
            is ProcessingOutcome.Failed -> append("[bad plugin] [${result.reason}]")
          }
        }
        append(progressData.task.presentableName)
      }
      reportage.logVerificationStage(message)
    }
    executor.executeTasks(tasks)
    return CountUsagesOfExtensionPointsTaskResult(extensionPointUsages, params.outputJson)
  }
}

class CountUsagesOfExtensionPointsTaskResult(
  private val extensionPointUsages: Map<String, Int>,
  private val outputJson: Path
) : TaskResult {

  private companion object {
    val jsonMapper = jacksonObjectMapper()
  }

  override fun createTaskResultsPrinter(pluginRepository: PluginRepository): TaskResultPrinter =
    object : TaskResultPrinter {
      override fun printResults(taskResult: TaskResult, outputOptions: OutputOptions) {
        val result = extensionPointUsages.map { it.key to it.value }.sortedByDescending { it.second }.map { arrayOf(it.first, it.second) }
        jsonMapper.writeValue(outputJson.toFile(), result)
        println("Result has been saved to $outputJson")
      }
    }
}