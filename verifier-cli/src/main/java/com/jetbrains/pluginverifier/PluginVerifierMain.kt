package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.PluginVerifierMain.commandRunners
import com.jetbrains.pluginverifier.PluginVerifierMain.main
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.ide.ReleaseIdeRepository
import com.jetbrains.pluginverifier.misc.createDir
import com.jetbrains.pluginverifier.misc.formatDuration
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsProviderImpl
import com.jetbrains.pluginverifier.plugin.PluginFilesBank
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.cleanup.SpaceUnit
import com.jetbrains.pluginverifier.repository.cleanup.bytesToSpaceAmount
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import com.jetbrains.pluginverifier.tasks.CommandRunner
import com.jetbrains.pluginverifier.tasks.checkIde.CheckIdeRunner
import com.jetbrains.pluginverifier.tasks.checkPlugin.CheckPluginRunner
import com.jetbrains.pluginverifier.tasks.checkPluginApi.CheckPluginApiRunner
import com.jetbrains.pluginverifier.tasks.checkTrunkApi.CheckTrunkApiRunner
import com.jetbrains.pluginverifier.tasks.deprecatedUsages.DeprecatedUsagesRunner
import com.sampullara.cli.Args
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
      DeprecatedUsagesRunner(),
      CheckPluginApiRunner()
  )

  private val verifierHomeDir: Path by lazy {
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

  private val downloadDir: Path = verifierHomeDir.resolve("loaded-plugins").createDir()

  private val extractDir: Path = verifierHomeDir.resolve("extracted-plugins").createDir()

  private val ideDownloadDir: Path = verifierHomeDir.resolve("ides").createDir()

  private val LOG: Logger = LoggerFactory.getLogger(PluginVerifierMain::class.java)

  @JvmStatic
  fun main(args: Array<String>) {
    val opts = CmdOpts()
    var freeArgs = Args.parse(opts, args, false)

    if (freeArgs.isEmpty()) {
      System.err.println(
          """The command is not specified. Should be one of 'check-plugin' or 'check-ide'.
  Example: java -jar verifier.jar -r /usr/lib/jvm/java-8-oracle check-plugin /tmp/Kotlin /tmp/IU-162.2032.8 /tmp/IU-163.1024 /tmp/IU-163.7277
        OR java -jar verifier.jar check-ide /tmp/IU-162.2032.8

  More examples on https://github.com/JetBrains/intellij-plugin-verifier/
"""
      )
      Args.usage(System.err, CmdOpts())

      exitProcess(1)
    }

    val command = freeArgs[0]
    freeArgs = freeArgs.drop(1)

    val runner = findTaskRunner(command)
    val outputOptions = OptionsParser.parseOutputOptions(opts)

    val pluginRepository = MarketplaceRepository(URL(pluginRepositoryUrl))
    val pluginDownloadDirDiskSpaceSetting = getDiskSpaceSetting("plugin.verifier.cache.dir.max.space", 5 * 1024)
    val pluginFilesBank = PluginFilesBank.create(pluginRepository, downloadDir, pluginDownloadDirDiskSpaceSetting)
    val pluginDetailsProvider = PluginDetailsProviderImpl(extractDir)

    val ideRepository = ReleaseIdeRepository()
    val ideFilesDiskSetting = getDiskSpaceSetting("plugin.verifier.cache.ide.dir.max.space", 10 * 1024)
    val ideFilesBank = IdeFilesBank(ideDownloadDir, ideRepository, ideFilesDiskSetting)

    VerificationReportage(outputOptions).use { reportage ->
      val taskResult = PluginDetailsCache(10, pluginFilesBank, pluginDetailsProvider).use { pluginDetailsCache ->

        runner.getParametersBuilder(
            pluginRepository,
            ideFilesBank,
            pluginDetailsCache,
            reportage
        ).build(opts, freeArgs).use { parameters ->
          reportage.logVerificationStage("Task ${runner.commandName} parameters:\n$parameters")

          val concurrentWorkers = getConcurrencyLevel()
          JdkDescriptorsCache().use { jdkDescriptorCache ->
            VerifierExecutor(concurrentWorkers, reportage).use { verifierExecutor ->
              runner
                  .createTask(parameters, pluginRepository, pluginDetailsCache)
                  .execute(reportage, verifierExecutor, jdkDescriptorCache, pluginDetailsCache)
            }
          }
        }
      }

      val taskResultsPrinter = runner.createTaskResultsPrinter(outputOptions, pluginRepository)
      taskResultsPrinter.printResults(taskResult)
      reportage.reportDownloadStatistics(outputOptions, pluginFilesBank)
    }
  }

  private fun VerificationReportage.reportDownloadStatistics(outputOptions: OutputOptions, pluginFilesBank: PluginFilesBank) {
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
      outputOptions.teamCityLog.buildStatisticValue("intellij.plugin.verifier.downloading.time.ms", totalDownloadDuration.toMillis())
      outputOptions.teamCityLog.buildStatisticValue("intellij.plugin.verifier.downloading.amount.bytes", totalDownloadedAmount.to(SpaceUnit.BYTE).toLong())
      outputOptions.teamCityLog.buildStatisticValue("intellij.plugin.verifier.total.space.used", totalSpaceUsed.to(SpaceUnit.BYTE).toLong())
    }
  }

  private fun getConcurrencyLevel(): Int {
    val fromProperty = System.getProperty("intellij.plugin.verifier.concurrency.level")?.toIntOrNull()
    if (fromProperty != null) {
      check(0 < fromProperty && fromProperty <= 64) { "Concurrency level must be between 1 and 64, but was $fromProperty" }
      return fromProperty
    }

    val availableMemory = Runtime.getRuntime().maxMemory().bytesToSpaceAmount()
    val availableCpu = Runtime.getRuntime().availableProcessors().toLong()
    /**
     * We assume that about 200 Mb is needed for an average verification
     */
    val maxByMemory = availableMemory.to(SpaceUnit.MEGA_BYTE).toLong() / 200
    val concurrencyLevel = maxOf(4, minOf(maxByMemory, availableCpu)).toInt()
    LOG.info("Available memory: $availableMemory; Available CPU = $availableCpu; Concurrency level = $concurrencyLevel")
    return concurrencyLevel
  }

  private fun getDiskSpaceSetting(propertyName: String, defaultAmount: Long): DiskSpaceSetting {
    val property = System.getProperty(propertyName)?.toLong() ?: defaultAmount
    val megabytes = SpaceAmount.ofMegabytes(property)
    return DiskSpaceSetting(megabytes)
  }

  private fun findTaskRunner(command: String?) = commandRunners.find { command == it.commandName }
      ?: throw IllegalArgumentException("Unsupported command: $command. Supported commands: ${commandRunners.map { it.commandName }}")

}
