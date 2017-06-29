package com.jetbrains.pluginverifier.tasks

import com.google.common.util.concurrent.AtomicDouble
import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.api.JdkDescriptor
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.repository.IdeRepository
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * @author Sergey Patrikeev
 */
class CheckTrunkApiParamsBuilder : TaskParametersBuilder<CheckTrunkApiParams> {

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

    if (apiOpts.majorIdePath != null) {
      majorIdeFile = File(apiOpts.majorIdePath)
      if (!majorIdeFile.isDirectory) {
        throw IllegalArgumentException("The specified major IDE doesn't exist: $majorIdeFile")
      }
      deleteMajorOnExit = false
    } else if (apiOpts.majorIdeVersion != null) {
      val ideVersion = parseIdeVersion(apiOpts.majorIdeVersion!!)
      majorIdeFile = downloadIdeByVersion(ideVersion)
      deleteMajorOnExit = true
    } else {
      throw IllegalArgumentException("Neither the version (-miv) nor the path to the IDE (-mip) with which to compare API problems specified")
    }

    val externalClassesPrefixes = OptionsParser.getExternalClassesPrefixes(opts)
    val problemsFilter = OptionsParser.getProblemsFilter(opts)

    val majorIdeDescriptor = OptionsParser.createIdeDescriptor(majorIdeFile, opts)
    return CheckTrunkApiParams(ideDescriptor, majorIdeDescriptor, externalClassesPrefixes, problemsFilter, jdkDescriptor, deleteMajorOnExit, majorIdeFile)
  }

  private fun downloadIdeByVersion(ideVersion: IdeVersion): File {
    val lastProgress = AtomicDouble()
    return IdeRepository.getOrDownloadIde(ideVersion) {
      if (it - lastProgress.get() > 0.1) {
        LOG.info("IDE #$ideVersion downloading progress ${(it * 100).toInt()}%")
        lastProgress.set(it)
      }
    }
  }

  private fun parseIdeVersion(ideVersion: String): IdeVersion {
    try {
      return IdeVersion.createIdeVersion(ideVersion.substringAfter("IU-").substringAfter("IC-"))
    } catch(e: Exception) {
      LOG.error("Unable to parse major ide version from $ideVersion", e)
      throw IllegalArgumentException("Please provide valid build number of major IDE version with which to compare API problems; " +
          "see https://www.jetbrains.com/intellij-repository/releases/", e)
    }
  }

  private class CheckTrunkApiOpts {
    @set:Argument("major-ide-version", alias = "miv", description = "The IDE version with which to compare API problems")
    var majorIdeVersion: String? = null

    @set:Argument("major-ide-path", alias = "mip", description = "The path to the IDE with which to compare API problems")
    var majorIdePath: String? = null
  }

}