package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.misc.createDir
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.plugin.PluginDetailsProviderImpl
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.reporting.common.LogReporter
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportageImpl
import com.jetbrains.pluginverifier.repository.IdeRepository
import com.jetbrains.pluginverifier.repository.PublicPluginRepository
import com.jetbrains.pluginverifier.tasks.TaskRunner
import com.jetbrains.pluginverifier.tasks.checkIde.CheckIdeRunner
import com.jetbrains.pluginverifier.tasks.checkPlugin.CheckPluginRunner
import com.jetbrains.pluginverifier.tasks.checkTrunkApi.CheckTrunkApiRunner
import com.sampullara.cli.Args
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.system.exitProcess

object PluginVerifierMain {

  private val taskRunners: List<TaskRunner> = listOf(CheckPluginRunner(), CheckIdeRunner(), CheckTrunkApiRunner())

  private val DEFAULT_IDE_REPOSITORY_URL = "https://jetbrains.com"

  private val DEFAULT_PLUGIN_REPOSITORY_URL = "https://plugins.jetbrains.com"

  private fun getVerifierHomeDir(): File {
    val verifierHomeDir = System.getProperty("plugin.verifier.home.dir")
    if (verifierHomeDir != null) {
      return File(verifierHomeDir)
    }
    val userHome = System.getProperty("user.home")
    if (userHome != null) {
      return File(userHome, ".pluginVerifier")
    }
    return File(FileUtils.getTempDirectory(), ".pluginVerifier")
  }

  private val ideRepositoryUrl: String by lazy {
    System.getProperty("ide.repository.url")?.trimEnd('/') ?: DEFAULT_IDE_REPOSITORY_URL ?: throw RuntimeException("IDE repository URL is not specified")
  }

  private val pluginRepositoryUrl: String by lazy {
    System.getProperty("plugin.repository.url")?.trimEnd('/') ?: DEFAULT_PLUGIN_REPOSITORY_URL ?: throw RuntimeException("Plugin repository URL is not specified")
  }

  private val downloadDirMaxSpace: Long? by lazy {
    System.getProperty("plugin.verifier.cache.dir.max.space")?.let { it.toLong() * FileUtils.ONE_MB }
  }


  private val downloadDir: File = File(getVerifierHomeDir(), "loaded-plugins").createDir()

  private val extractDir: File = File(getVerifierHomeDir(), "extracted-plugins").createDir()

  private val ideDownloadDir: File = File(getVerifierHomeDir(), "ides").createDir()

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

    val downloadDirMaxSpace = downloadDirMaxSpace ?: 5 * FileUtils.ONE_GB
    val pluginRepository = PublicPluginRepository(pluginRepositoryUrl, downloadDir, downloadDirMaxSpace)
    val ideRepository = IdeRepository(ideDownloadDir, ideRepositoryUrl)
    val pluginDetailsProvider = PluginDetailsProviderImpl(extractDir)

    val runner = findTaskRunner(command)
    val parametersBuilder = runner.getParametersBuilder(pluginRepository, ideRepository, pluginDetailsProvider)

    val verificationReportsDirectory = OptionsParser.getVerificationReportsDirectory(opts)
    println("Verification reports directory: $verificationReportsDirectory")

    val parameters = try {
      parametersBuilder.build(opts, freeArgs)
    } catch (e: IllegalArgumentException) {
      System.err.println(e.message)
      exitProcess(1)
    }

    val taskResult = parameters.use {
      println("Task ${runner.commandName} parameters: $parameters")
      createVerificationReportage(verificationReportsDirectory, opts.printPluginVerificationProgress).use { verificationReportage ->
        runner
            .createTask(parameters, pluginRepository, pluginDetailsProvider)
            .execute(verificationReportage)
      }
    }

    val printerOptions = OptionsParser.parseOutputOptions(opts)
    taskResult.printResults(printerOptions, pluginRepository)
  }

  private fun createVerificationReportage(verificationReportsDirectory: File, printPluginVerificationProgress: Boolean): VerificationReportage {
    val logger = LoggerFactory.getLogger("verification")
    val messageReporters = listOf(LogReporter<String>(logger))
    val progressReporters = emptyList<Reporter<Double>>()

    val reporterSetProvider = MainVerificationReportersProvider(messageReporters, progressReporters, verificationReportsDirectory, printPluginVerificationProgress)
    return VerificationReportageImpl(reporterSetProvider)
  }

  private fun findTaskRunner(command: String?) = taskRunners.find { command == it.commandName }
      ?: throw IllegalArgumentException("Unsupported command: $command. Supported commands: ${taskRunners.map { it.commandName }}")

}
