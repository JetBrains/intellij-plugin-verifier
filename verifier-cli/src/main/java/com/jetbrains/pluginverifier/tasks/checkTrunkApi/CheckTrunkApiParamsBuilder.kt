package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.google.common.util.concurrent.AtomicDouble
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptor
import com.jetbrains.pluginverifier.repository.IdeRepository
import com.jetbrains.pluginverifier.tasks.TaskParametersBuilder
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * @author Sergey Patrikeev
 */
class CheckTrunkApiParamsBuilder(val ideRepository: IdeRepository) : TaskParametersBuilder {

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(CheckTrunkApiParamsBuilder::class.java)
  }

  override fun build(opts: CmdOpts, freeArgs: List<String>): CheckTrunkApiParams {
    val apiOpts = CheckTrunkApiOpts()
    val args = Args.parse(apiOpts, freeArgs.toTypedArray(), false)
    if (args.isEmpty()) {
      throw IllegalArgumentException("The IDE to be checked is not specified")
    }

    val ideDescriptor = OptionsParser.createIdeDescriptor(File(args[0]), opts)
    val jdkDescriptor = JdkDescriptor(OptionsParser.getJdkDir(opts))

    val majorIdeFile: File
    val deleteMajorOnExit: Boolean

    when {
      apiOpts.majorIdePath != null -> {
        majorIdeFile = File(apiOpts.majorIdePath)
        if (!majorIdeFile.isDirectory) {
          throw IllegalArgumentException("The specified major IDE doesn't exist: $majorIdeFile")
        }
        deleteMajorOnExit = false
      }
      apiOpts.majorIdeVersion != null -> {
        val ideVersion = parseIdeVersion(apiOpts.majorIdeVersion!!)
        majorIdeFile = downloadIdeByVersion(ideVersion)
        deleteMajorOnExit = !apiOpts.saveMajorIdeFile
      }
      else -> throw IllegalArgumentException("Neither the version (-miv) nor the path to the IDE (-mip) with which to compare API problems specified")
    }

    val externalClassesPrefixes = OptionsParser.getExternalClassesPrefixes(opts)
    val problemsFilters = OptionsParser.getProblemsFilters(opts)

    val majorIdeDescriptor = OptionsParser.createIdeDescriptor(majorIdeFile, opts)

    val jetBrainsPluginIds = getJetBrainsPluginIds(apiOpts)
    return CheckTrunkApiParams(ideDescriptor, majorIdeDescriptor, externalClassesPrefixes, problemsFilters, jdkDescriptor, jetBrainsPluginIds, deleteMajorOnExit, majorIdeFile)
  }

  private fun getJetBrainsPluginIds(apiOpts: CheckTrunkApiOpts): List<String> {
    if (apiOpts.jetBrainsPluginsFile != null) {
      return File(apiOpts.jetBrainsPluginsFile).readLines()
    }
    return emptyList()
  }

  private fun downloadIdeByVersion(ideVersion: IdeVersion): File {
    val lastProgress = AtomicDouble()
    return ideRepository.getOrDownloadIde(ideVersion) {
      if (it - lastProgress.get() > 0.1) {
        LOG.info("IDE #$ideVersion downloading progress ${(it * 100).toInt()}%")
        lastProgress.set(it)
      }
    }
  }

  private fun parseIdeVersion(ideVersion: String): IdeVersion {
    try {
      return IdeVersion.createIdeVersion(ideVersion)
    } catch(e: Exception) {
      throw IllegalArgumentException("Invalid IDE version: $ideVersion. Please provide IDE version (with product ID) with which to compare API problems; " +
          "See https://www.jetbrains.com/intellij-repository/releases/", e)
    }
  }

  class CheckTrunkApiOpts {
    @set:Argument("major-ide-version", alias = "miv", description = "The IDE version with which to compare API problems")
    var majorIdeVersion: String? = null

    @set:Argument("save-major-ide-file", alias = "smif", description = "Whether to save a downloaded release IDE in cache directory for use in later verifications")
    var saveMajorIdeFile: Boolean = false

    @set:Argument("major-ide-path", alias = "mip", description = "The path to the IDE with which to compare API problems")
    var majorIdePath: String? = null

    @set:Argument("jetbrains-plugins-file", alias = "jbpf", description = "The path to a file with plugin ids separated by newline. " +
        "The provided plugin ids are JetBrains-developed plugins that in conjunction with IDE build constitute IntelliJ API used by third-party plugin developers. " +
        "Compatible versions of these plugins will be downloaded and installed to the release and trunk IDE before verification. " +
        "Found compatibility problems differences will be reported as if it were breakages of trunk API compared to release API.")
    var jetBrainsPluginsFile: String? = null
  }

}