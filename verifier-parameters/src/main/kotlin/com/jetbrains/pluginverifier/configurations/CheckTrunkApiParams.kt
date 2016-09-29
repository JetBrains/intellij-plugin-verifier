package com.jetbrains.pluginverifier.configurations

import com.intellij.structure.domain.Ide
import com.jetbrains.pluginverifier.api.JdkDescriptor
import com.jetbrains.pluginverifier.api.VOptions
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.misc.extractTo
import com.jetbrains.pluginverifier.repository.IdeRepository
import com.jetbrains.pluginverifier.utils.CmdOpts
import com.jetbrains.pluginverifier.utils.CmdUtil
import com.jetbrains.pluginverifier.utils.VOptionsUtil
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files

/**
 * @author Sergey Patrikeev
 */
object CheckTrunkApiParamsParser : ParamsParser {

  private val LOG: Logger = LoggerFactory.getLogger(CheckTrunkApiParamsParser::class.java)

  override fun parse(opts: CmdOpts, freeArgs: List<String>): CheckTrunkApiParams {
    val apiOpts = CheckTrunkApiOpts()
    val args = Args.parse(apiOpts, freeArgs.toTypedArray(), false)
    if (args.isEmpty()) {
      throw IllegalArgumentException("The IDE to be checked is not specified")
    }

    val ide = CmdUtil.createIde(File(args[0]), opts)
    val jdkDescriptor = JdkDescriptor.ByFile(CmdUtil.getJdkDir(opts))

    val majorIdeFile: File
    val deleteMajorOnExit: Boolean

    if (apiOpts.majorIdePath != null) {
      majorIdeFile = File(apiOpts.majorIdePath)
      if (!majorIdeFile.isDirectory) {
        throw IllegalArgumentException("The specified major IDE doesn't exist: $majorIdeFile")
      }
      deleteMajorOnExit = false
    } else if (apiOpts.majorIdeVersion != null) {
      majorIdeFile = downloadIde(apiOpts.majorIdeVersion!!)
      deleteMajorOnExit = true
    } else {
      throw IllegalArgumentException("Neither the version (-miv) nor the path to the IDE (-mip) with which to compare API problems are not specified")
    }

    return CheckTrunkApiParams(ide, majorIdeFile, deleteMajorOnExit, VOptionsUtil.parseOpts(opts), jdkDescriptor)
  }

  private fun downloadIde(ideVersion: String): File {
    val ideZip = downloadIdeZip(ideVersion)

    try {
      val tempIdeDir = Files.createTempDirectory(FileUtils.getTempDirectory().toPath(), "ide").toFile()

      try {
        return ideZip.extractTo(tempIdeDir)
      } catch (e: Exception) {
        LOG.error("Unable to extract IDE $ideVersion from file $ideZip to $tempIdeDir", e)
        tempIdeDir.deleteLogged()
        throw e
      }

    } finally {
      ideZip.deleteLogged()
    }
  }

  private fun downloadIdeZip(ideVersion: String): File {
    val tempFile = File.createTempFile("ide", ".zip")
    try {
      val isCommunity = ideVersion.startsWith("IC-")
      val rawVersion = ideVersion.substringAfter("IU-").substringAfter("IC-")

      val fromReleases = IdeRepository.fetchIndex().find { it.version == rawVersion && it.isCommunity == isCommunity }
      if (fromReleases != null) {
        return IdeRepository.downloadIde(fromReleases, tempFile)
      }

      val fromSnapshots = IdeRepository.fetchIndex(true).find { it.version == rawVersion && it.isCommunity == isCommunity }
      if (fromSnapshots != null) {
        return IdeRepository.downloadIde(fromSnapshots, tempFile)
      }

      throw IllegalArgumentException("The IDE #$rawVersion is not found neither in releases nor in the snapshots IDE repository")
    } catch (e: Exception) {
      tempFile.deleteLogged()
      throw e
    }
  }

}


data class CheckTrunkApiParams(val ide: Ide,
                               val majorIdeFile: File,
                               val deleteMajorIdeOnExit: Boolean,
                               val vOptions: VOptions,
                               val jdkDescriptor: JdkDescriptor) : Params

class CheckTrunkApiOpts {
  @set:Argument("major-ide-version", alias = "miv", description = "The IDE version with which to compare API problems")
  var majorIdeVersion: String? = null

  @set:Argument("major-ide-path", alias = "mip", description = "The path to the IDE with which to compare API problems")
  var majorIdePath: String? = null
}