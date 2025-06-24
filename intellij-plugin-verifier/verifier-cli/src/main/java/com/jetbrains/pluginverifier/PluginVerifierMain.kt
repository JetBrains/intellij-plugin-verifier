/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier

import com.jetbrains.plugin.structure.base.utils.createDir
import com.jetbrains.plugin.structure.base.utils.forceDeleteIfExists
import com.jetbrains.plugin.structure.base.utils.formatDuration
import com.jetbrains.plugin.structure.intellij.plugin.caches.SimplePluginResourceCache
import com.jetbrains.pluginverifier.PluginVerifierMain.commandRunners
import com.jetbrains.pluginverifier.PluginVerifierMain.main
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.options.repository.LocalPluginRepositoryProvider
import com.jetbrains.pluginverifier.options.repository.LocalPluginRepositoryProvider.Result.Provided
import com.jetbrains.pluginverifier.options.repository.LocalPluginRepositoryProvider.Result.Unavailable
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.plugin.DefaultPluginDetailsProvider
import com.jetbrains.pluginverifier.plugin.PluginFilesBank
import com.jetbrains.pluginverifier.plugin.SizeLimitedPluginDetailsCache
import com.jetbrains.pluginverifier.reporting.DirectoryBasedPluginVerificationReportage
import com.jetbrains.pluginverifier.reporting.LoggingPluginVerificationReportageAggregator
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.cleanup.SpaceUnit
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import com.jetbrains.pluginverifier.tasks.CommandRunner
import com.jetbrains.pluginverifier.tasks.checkIde.CheckIdeRunner
import com.jetbrains.pluginverifier.tasks.checkPlugin.CheckPluginRunner
import com.jetbrains.pluginverifier.tasks.checkPluginApi.CheckPluginApiRunner
import com.jetbrains.pluginverifier.tasks.checkTrunkApi.CheckTrunkApiRunner
import com.jetbrains.pluginverifier.tasks.processAllPlugins.ProcessAllPluginsCommand
import com.jetbrains.pluginverifier.tasks.profiling.MeasuredResult
import com.jetbrains.pluginverifier.tasks.profiling.measurePluginVerification
import com.sampullara.cli.Args
import org.apache.commons.io.FileUtils
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess

/**
 * The plugin verifier CLI entry point declaring the [main].
 * The available commands are [commandRunners].
 */
object PluginVerifierMain {

  private val commandRunners: List<CommandRunner> = listOf(
    CheckPluginRunner(),
    CheckIdeRunner(),
    CheckTrunkApiRunner(),
    CheckPluginApiRunner(),
    ProcessAllPluginsCommand()
  )

  private val pluginVerifierVersion: String by lazy {
    val versionTxtUrl = PluginVerifierMain::class.java.getResource("/META-INF/intellij-plugin-verifier-version.txt")
    versionTxtUrl ?: return@lazy "<unknown>"
    versionTxtUrl.readText()
  }

  private val verifierHomeDirectory: Path by lazy {
    val verifierHomeDir = System.getProperty("plugin.verifier.home.dir")
    if (verifierHomeDir != null) {
      Paths.get(verifierHomeDir)
    } else {
      val userHome = System.getProperty("user.home")
      if (userHome != null) {
        Paths.get(userHome, ".pluginVerifier")
      } else {
        FileUtils.getTempDirectory().toPath().resolve(".pluginVerifier")
      }
    }
  }

  private val pluginRepositoryUrl: String by lazy {
    System.getProperty("plugin.repository.url")?.trimEnd('/')
      ?: "https://plugins.jetbrains.com"
  }

  private val downloadDirectory: Path = verifierHomeDirectory.resolve("loaded-plugins").createDir()

  private fun getPluginsExtractDirectory(): Path {
    val extractDirectory = verifierHomeDirectory.resolve("extracted-plugins").createDir()
    extractDirectory.forceDeleteIfExists()
    return extractDirectory
  }

  private val ideDownloadDirectory: Path = verifierHomeDirectory.resolve("ides").createDir()

  @JvmStatic
  fun main(args: Array<String>) {
    println("Starting the IntelliJ Plugin Verifier $pluginVerifierVersion")
    val opts = CmdOpts()
    var freeArgs = Args.parse(opts, args, false)

    if (freeArgs.isEmpty()) {
      System.err.println(
        "The command is not specified. Should be one of: " + commandRunners.joinToString { "'" + it.commandName + "'" }
      )
      Args.usage(System.err, CmdOpts())

      exitProcess(1)
    }

    val command = freeArgs[0]
    freeArgs = freeArgs.drop(1)

    val runner = findTaskRunner(command)
    val outputOptions = OptionsParser.parseOutputOptions(opts)

    val pluginRepository =
      when (val repositoryProvision = LocalPluginRepositoryProvider.getLocalPluginRepository(opts, downloadDirectory)) {
        is Provided -> repositoryProvision.pluginRepository
        Unavailable -> MarketplaceRepository(URL(pluginRepositoryUrl))
      }

    val pluginDownloadDirDiskSpaceSetting = getDiskSpaceSetting("plugin.verifier.cache.dir.max.space", 5L * 1024)
    val pluginFilesBank = PluginFilesBank.create(pluginRepository, downloadDirectory, pluginDownloadDirDiskSpaceSetting)
    SimplePluginResourceCache().use { extractedPluginCache ->
      DefaultPluginDetailsProvider(getPluginsExtractDirectory(), extractedPluginCache).use { pluginDetailsProvider ->
        val reportageAggregator = LoggingPluginVerificationReportageAggregator()
        DirectoryBasedPluginVerificationReportage(reportageAggregator) { outputOptions.getTargetReportDirectory(it) }.use { reportage ->
          measurePluginVerification {
            val detailsCacheSize = System.getProperty("plugin.verifier.plugin.details.cache.size")?.toIntOrNull() ?: 32
            val taskResult = SizeLimitedPluginDetailsCache(
              detailsCacheSize,
              pluginFilesBank,
              pluginDetailsProvider
            ).use { pluginDetailsCache ->
              runner.getParametersBuilder(
                pluginRepository,
                pluginDetailsCache,
                extractedPluginCache,
                reportage
              ).build(opts, freeArgs).use { parameters ->
                reportage.logVerificationStage("Task ${runner.commandName} parameters:\n${parameters.presentableText}")

                parameters
                  .createTask()
                  .execute(reportage, pluginDetailsCache)
              }
            }
            taskResult
          }.run {
            val taskResultsPrinter = taskResult.createTaskResultsPrinter(pluginRepository)
            taskResultsPrinter.printResults(taskResult, outputOptions)
            reportage.reportDownloadStatistics(outputOptions, pluginFilesBank)
            reportageAggregator.handleAggregatedReportage()
            reportage.reportVerificationDuration(this)
          }
        }
      }
    }
  }

  private fun PluginVerificationReportage.reportDownloadStatistics(
    outputOptions: OutputOptions,
    pluginFilesBank: PluginFilesBank
  ) {
    val downloadStatistics = pluginFilesBank.downloadStatistics
    val totalSpaceUsed = pluginFilesBank.getAvailablePluginFiles().fold(SpaceAmount.ZERO_SPACE) { acc, availableFile ->
      acc + availableFile.resourceInfo.weight.spaceAmount
    }

    val totalDownloadedAmount = downloadStatistics.getTotalDownloadedAmount()
    val totalDownloadDuration = downloadStatistics.getTotalAstronomicalDownloadDuration()

    logVerificationStage("Total time spent downloading plugins and their dependencies: ${totalDownloadDuration.formatDuration()}")
    logVerificationStage("Total amount of plugins and dependencies downloaded: ${totalDownloadedAmount.presentableAmount()}")
    logVerificationStage("Total amount of space used for plugins and dependencies: ${totalSpaceUsed.presentableAmount()}")
    if (outputOptions.teamCityLog != null) {
      outputOptions.teamCityLog.buildStatisticValue(
        "intellij.plugin.verifier.downloading.time.ms",
        totalDownloadDuration.toMillis()
      )
      outputOptions.teamCityLog.buildStatisticValue(
        "intellij.plugin.verifier.downloading.amount.bytes",
        totalDownloadedAmount.to(SpaceUnit.BYTE).toLong()
      )
      outputOptions.teamCityLog.buildStatisticValue(
        "intellij.plugin.verifier.total.space.used",
        totalSpaceUsed.to(SpaceUnit.BYTE).toLong()
      )
    }
  }

  private fun PluginVerificationReportage.reportVerificationDuration(measuredResult: MeasuredResult) {
    logVerificationStage("Total time spent in plugin verification: ${measuredResult.duration.formatDuration()}")
  }

  private fun getDiskSpaceSetting(propertyName: String, defaultAmount: Long): DiskSpaceSetting {
    val property = System.getProperty(propertyName)?.toLong() ?: defaultAmount
    val megabytes = SpaceAmount.ofMegabytes(property)
    return DiskSpaceSetting(megabytes)
  }

  private fun findTaskRunner(command: String?) = commandRunners.find { command == it.commandName }
    ?: throw IllegalArgumentException("Unsupported command: $command. Supported commands: ${commandRunners.map { it.commandName }}")

}
