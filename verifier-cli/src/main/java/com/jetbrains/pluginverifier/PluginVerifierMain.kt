package com.jetbrains.pluginverifier

import com.google.common.util.concurrent.AtomicDouble
import com.jetbrains.pluginverifier.PluginVerifierMain.commandRunners
import com.jetbrains.pluginverifier.PluginVerifierMain.main
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.ide.IdeRepository
import com.jetbrains.pluginverifier.misc.createDir
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsProviderImpl
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.reporting.common.LogReporter
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportageImpl
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.PublicPluginRepository
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.tasks.CommandRunner
import com.jetbrains.pluginverifier.tasks.checkIde.CheckIdeRunner
import com.jetbrains.pluginverifier.tasks.checkPlugin.CheckPluginRunner
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
      DeprecatedUsagesRunner()
  )

  private val DEFAULT_IDE_REPOSITORY_URL = "https://jetbrains.com"

  private val DEFAULT_PLUGIN_REPOSITORY_URL = "https://plugins.jetbrains.com"

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

  private val ideRepositoryUrl: String by lazy {
    System.getProperty("ide.repository.url")?.trimEnd('/')
        ?: DEFAULT_IDE_REPOSITORY_URL
  }

  private val pluginRepositoryUrl: String by lazy {
    System.getProperty("plugin.repository.url")?.trimEnd('/')
        ?: DEFAULT_PLUGIN_REPOSITORY_URL
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
        OR java -jar verifier.jar -html-report report.html check-ide /tmp/IU-162.2032.8

  More examples on https://github.com/JetBrains/intellij-plugin-verifier/
""")
      Args.usage(System.err, CmdOpts())

      exitProcess(1)
    }

    val command = freeArgs[0]
    freeArgs = freeArgs.drop(1)

    val pluginDownloadDirDiskSpaceSetting = getPluginDownloadDirDiskSpaceSetting()
    val pluginRepository = PublicPluginRepository(URL(pluginRepositoryUrl), downloadDir, pluginDownloadDirDiskSpaceSetting)

    val ideRepository = IdeRepository(ideRepositoryUrl)

    val ideFilesDiskSetting = getIdeDownloadDirDiskSpaceSetting()
    val ideFilesBank = IdeFilesBank(ideDownloadDir, ideRepository, ideFilesDiskSetting, getIdeDownloadProgressListener())
    val pluginDetailsProvider = PluginDetailsProviderImpl(extractDir)
    PluginDetailsCache(10, pluginDetailsProvider).use {
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

    createVerificationReportage(
        verificationReportsDirectory,
        opts.printPluginVerificationProgress
    ).use { verificationReportage ->

      val runner = findTaskRunner(command)
      val parametersBuilder = runner.getParametersBuilder(pluginRepository, ideFilesBank, pluginDetailsCache, verificationReportage)

      val parameters = try {
        parametersBuilder.build(opts, freeArgs)
      } catch (e: IllegalArgumentException) {
        LOG.error("Unable to prepare verification parameters", e)
        System.err.println(e.message)
        exitProcess(1)
      }

      val taskResult = parameters.use {
        println("Task ${runner.commandName} parameters: $parameters")

        runner
            .createTask(parameters, pluginRepository, pluginDetailsCache)
            .execute(verificationReportage)
      }

      val outputOptions = OptionsParser.parseOutputOptions(opts, verificationReportsDirectory)
      val taskResultsPrinter = runner.createTaskResultsPrinter(outputOptions, pluginRepository)
      taskResultsPrinter.printResults(taskResult)

    }
  }

  private fun getIdeDownloadProgressListener(): (Double) -> Unit {
    val lastProgress = AtomicDouble()
    return { currentProgress ->
      if (currentProgress == 1.0 || currentProgress - lastProgress.get() > 0.1) {
        LOG.info("IDE downloading progress ${(currentProgress * 100).toInt()}%")
        lastProgress.set(currentProgress)
      }
    }
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

  private fun createVerificationReportage(verificationReportsDirectory: Path,
                                          printPluginVerificationProgress: Boolean): VerificationReportage {
    val logger = LoggerFactory.getLogger("verification")
    val messageReporters = listOf(LogReporter<String>(logger))
    val progressReporters = emptyList<Reporter<Double>>()

    val reporterSetProvider = MainVerificationReportersProvider(messageReporters, progressReporters, verificationReportsDirectory, printPluginVerificationProgress)
    return VerificationReportageImpl(reporterSetProvider)
  }

  private fun findTaskRunner(command: String?) = commandRunners.find { command == it.commandName }
      ?: throw IllegalArgumentException("Unsupported command: $command. Supported commands: ${commandRunners.map { it.commandName }}")

}
