package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.PluginVerifierMain.commandRunners
import com.jetbrains.pluginverifier.PluginVerifierMain.main
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.ide.ReleaseIdeRepository
import com.jetbrains.pluginverifier.misc.createDir
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsProviderImpl
import com.jetbrains.pluginverifier.plugin.PluginFilesBank
import com.jetbrains.pluginverifier.reporting.common.LogReporter
import com.jetbrains.pluginverifier.reporting.ignoring.IgnoredPluginsReporter
import com.jetbrains.pluginverifier.reporting.verification.Reportage
import com.jetbrains.pluginverifier.reporting.verification.ReportageImpl
import com.jetbrains.pluginverifier.repository.PluginRepository
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
      System.err.println("""The command is not specified. Should be one of 'check-plugin' or 'check-ide'.
  Example: java -jar verifier.jar -r /usr/lib/jvm/java-8-oracle check-plugin /tmp/Kotlin /tmp/IU-162.2032.8 /tmp/IU-163.1024 /tmp/IU-163.7277
        OR java -jar verifier.jar check-ide /tmp/IU-162.2032.8

  More examples on https://github.com/JetBrains/intellij-plugin-verifier/
""")
      Args.usage(System.err, CmdOpts())

      exitProcess(1)
    }

    val command = freeArgs[0]
    freeArgs = freeArgs.drop(1)

    val pluginDownloadDirDiskSpaceSetting = getPluginDownloadDirDiskSpaceSetting()
    val pluginRepository = MarketplaceRepository(URL(pluginRepositoryUrl))
    val pluginFilesBank = PluginFilesBank.create(pluginRepository, downloadDir, pluginDownloadDirDiskSpaceSetting)

    val ideRepository = ReleaseIdeRepository()

    val ideFilesDiskSetting = getIdeDownloadDirDiskSpaceSetting()
    val ideFilesBank = IdeFilesBank(ideDownloadDir, ideRepository, ideFilesDiskSetting)
    val pluginDetailsProvider = PluginDetailsProviderImpl(extractDir)
    PluginDetailsCache(10, pluginFilesBank, pluginDetailsProvider).use {
      runVerification(command, freeArgs, pluginRepository, ideFilesBank, it, opts)
    }
  }

  private fun runVerification(
      command: String,
      freeArgs: List<String>,
      pluginRepository: PluginRepository,
      ideFilesBank: IdeFilesBank,
      pluginDetailsCache: PluginDetailsCache,
      opts: CmdOpts
  ) {
    val verificationReportsDirectory = OptionsParser.getVerificationReportsDirectory(opts)
    println("Verification reports directory: $verificationReportsDirectory")

    createReportage(
        verificationReportsDirectory
    ).use { reportage ->

      val runner = findTaskRunner(command)
      val parametersBuilder = runner.getParametersBuilder(pluginRepository, ideFilesBank, pluginDetailsCache, reportage)

      val parameters = try {
        parametersBuilder.build(opts, freeArgs)
      } catch (e: IllegalArgumentException) {
        LOG.error("Unable to prepare verification parameters", e)
        System.err.println(e.message)
        exitProcess(1)
      }

      val taskResult = parameters.use {
        println("Task ${runner.commandName} parameters:\n$parameters")

        val concurrentWorkers = estimateNumberOfConcurrentWorkers()
        JdkDescriptorsCache().use { jdkDescriptorCache ->
          VerifierExecutor(concurrentWorkers).use { verifierExecutor ->
            runner
                .createTask(parameters, pluginRepository, pluginDetailsCache)
                .execute(reportage, verifierExecutor, jdkDescriptorCache, pluginDetailsCache)
          }
        }
      }

      val outputOptions = OptionsParser.parseOutputOptions(opts, verificationReportsDirectory)
      val taskResultsPrinter = runner.createTaskResultsPrinter(outputOptions, pluginRepository)
      taskResultsPrinter.printResults(taskResult)

    }
  }

  private fun estimateNumberOfConcurrentWorkers(): Int {
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

  private fun getIdeDownloadDirDiskSpaceSetting(): DiskSpaceSetting =
      ofMegabytes("plugin.verifier.cache.ide.dir.max.space", 5 * 1024)

  private fun getPluginDownloadDirDiskSpaceSetting(): DiskSpaceSetting =
      ofMegabytes("plugin.verifier.cache.dir.max.space", 5 * 1024)

  private fun ofMegabytes(propertyName: String, defaultAmount: Long): DiskSpaceSetting {
    val property = System.getProperty(propertyName)?.toLong() ?: defaultAmount
    val megabytes = SpaceAmount.ofMegabytes(property)
    return DiskSpaceSetting(megabytes)
  }

  private fun createReportage(verificationReportsDirectory: Path): Reportage {
    val logger = LoggerFactory.getLogger("verification")
    val messageReporters = listOf(LogReporter<String>(logger))
    val reportersProvider = DirectoryLayoutReportersProvider(verificationReportsDirectory)
    val ignoredPluginsReporter = IgnoredPluginsReporter(verificationReportsDirectory)
    return ReportageImpl(reportersProvider, messageReporters, ignoredPluginsReporter)
  }

  private fun findTaskRunner(command: String?) = commandRunners.find { command == it.commandName }
      ?: throw IllegalArgumentException("Unsupported command: $command. Supported commands: ${commandRunners.map { it.commandName }}")

}
