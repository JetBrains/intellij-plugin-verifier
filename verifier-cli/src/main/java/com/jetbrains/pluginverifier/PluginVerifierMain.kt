package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.api.DefaultProgress
import com.jetbrains.pluginverifier.misc.createDir
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.options.PublicOpts
import com.jetbrains.pluginverifier.plugin.PluginCreatorImpl
import com.jetbrains.pluginverifier.repository.IdeRepository
import com.jetbrains.pluginverifier.repository.PublicPluginRepository
import com.jetbrains.pluginverifier.tasks.CheckIdeRunner
import com.jetbrains.pluginverifier.tasks.CheckPluginRunner
import com.jetbrains.pluginverifier.tasks.CheckTrunkApiRunner
import com.jetbrains.pluginverifier.tasks.TaskRunner
import com.sampullara.cli.Args
import org.apache.commons.io.FileUtils
import java.io.File

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
      Args.usage(System.err, PublicOpts())

      System.exit(1)
    }

    val command = freeArgs[0]
    freeArgs = freeArgs.drop(1)

    val downloadDirMaxSpace = downloadDirMaxSpace ?: 5 * FileUtils.ONE_GB
    val pluginRepository = PublicPluginRepository(pluginRepositoryUrl, downloadDir, downloadDirMaxSpace)
    val ideRepository = IdeRepository(ideDownloadDir, ideRepositoryUrl)
    val pluginCreator = PluginCreatorImpl(pluginRepository, extractDir)

    val runner = findTaskRunner(command)
    val result = runner.runTask(opts, freeArgs, pluginRepository, ideRepository, pluginCreator, DefaultProgress())

    val printerOptions = OptionsParser.parsePrinterOptions(opts)
    result.printResults(printerOptions, pluginRepository)
  }

  private fun findTaskRunner(command: String?) = taskRunners.find { command == it.commandName }
      ?: throw IllegalArgumentException("Unsupported command: $command. Supported commands: ${taskRunners.map { it.commandName }}")

}
