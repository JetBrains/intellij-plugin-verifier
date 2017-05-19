package com.jetbrains.pluginverifier.configurations

import com.google.common.util.concurrent.AtomicDouble
import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.JdkDescriptor
import com.jetbrains.pluginverifier.api.ProblemsFilter
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.misc.extractTo
import com.jetbrains.pluginverifier.repository.IdeRepository
import com.jetbrains.pluginverifier.repository.RepositoryConfiguration
import com.jetbrains.pluginverifier.utils.CmdOpts
import com.jetbrains.pluginverifier.utils.OptionsUtil
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.util.function.Function

/**
 * @author Sergey Patrikeev
 */
object CheckTrunkApiParamsParser : ConfigurationParamsParser {

  private val LOG: Logger = LoggerFactory.getLogger(CheckTrunkApiParamsParser::class.java)

  override fun parse(opts: CmdOpts, freeArgs: List<String>): CheckTrunkApiParams {
    val apiOpts = CheckTrunkApiOpts()
    val args = Args.parse(apiOpts, freeArgs.toTypedArray(), false)
    if (args.isEmpty()) {
      throw IllegalArgumentException("The IDE to be checked is not specified")
    }

    val ideDescriptor = OptionsUtil.createIdeDescriptor(File(args[0]), opts)
    val jdkDescriptor = JdkDescriptor(OptionsUtil.getJdkDir(opts))

    val majorIdeFile: File
    val deleteMajorOnExit: Boolean

    if (apiOpts.majorIdePath != null) {
      majorIdeFile = File(apiOpts.majorIdePath)
      if (!majorIdeFile.isDirectory) {
        throw IllegalArgumentException("The specified major IDE doesn't exist: $majorIdeFile")
      }
      deleteMajorOnExit = false
    } else if (apiOpts.majorIdeVersion != null) {
      majorIdeFile = downloadIde(parseIdeVersion(apiOpts.majorIdeVersion!!))
      deleteMajorOnExit = true
    } else {
      throw IllegalArgumentException("Neither the version (-miv) nor the path to the IDE (-mip) with which to compare API problems specified")
    }

    val externalClassesPrefixes = OptionsUtil.getExternalClassesPrefixes(opts)
    val problemsFilter = OptionsUtil.getProblemsFilter(opts)

    val majorIdeDescriptor = OptionsUtil.createIdeDescriptor(majorIdeFile, opts)
    return CheckTrunkApiParams(ideDescriptor, majorIdeDescriptor, externalClassesPrefixes, problemsFilter, jdkDescriptor, deleteMajorOnExit)
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

  private fun downloadIde(ideVersion: IdeVersion): File {

    LOG.info("Downloading the IDE #$ideVersion")
    val ideZip = downloadIdeZip(ideVersion)
    LOG.info("Successfully downloaded to $ideZip")

    try {
      val tempIdeDir = Files.createTempDirectory(RepositoryConfiguration.ideDownloadDir.toPath(), "ide").toFile()

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

  private fun downloadIdeZip(ideVersion: IdeVersion): File {
    val tempFile = File.createTempFile("ide", ".zip", RepositoryConfiguration.ideDownloadDir)

    val lastProgress = AtomicDouble()
    val progressUpdater = Function<Double, Unit> {
      if (it - lastProgress.get() > 0.1) {
        LOG.info("Downloading progress is ${(it * 100).toInt()}%")
        lastProgress.set(it)
      }
    }

    try {
      val fromReleases = IdeRepository.fetchIndex().find { it.version == ideVersion }
      if (fromReleases != null) {
        return IdeRepository.downloadIde(fromReleases, tempFile, progressUpdater)
      }

      val fromSnapshots = IdeRepository.fetchIndex(true).find { it.version == ideVersion }
      if (fromSnapshots != null) {
        return IdeRepository.downloadIde(fromSnapshots, tempFile, progressUpdater)
      }

      throw IllegalArgumentException("The IDE #$ideVersion is not found neither in releases nor in the snapshots IDE repository")
    } catch (e: Exception) {
      tempFile.deleteLogged()
      throw e
    }
  }

}


data class CheckTrunkApiParams(val ideDescriptor: IdeDescriptor,
                               val majorIdeDescriptor: IdeDescriptor,
                               val externalClassesPrefixes: List<String>,
                               val problemsFilter: ProblemsFilter,
                               val jdkDescriptor: JdkDescriptor,
                               private val deleteMajorIdeOnExit: Boolean) : ConfigurationParams {
  override fun presentableText(): String = """Check Trunk API Configuration Parameters:
Trunk IDE to be checked: $ideDescriptor
Release IDE to compare API with: $majorIdeDescriptor
External classes prefixes: [${externalClassesPrefixes.joinToString()}]
JDK: $jdkDescriptor
"""

  override fun close() {
    try {
      ideDescriptor.close()
    } finally {
      majorIdeDescriptor.close()
    }
  }

  override fun toString(): String = presentableText()
}

class CheckTrunkApiOpts {
  @set:Argument("major-ide-version", alias = "miv", description = "The IDE version with which to compare API problems")
  var majorIdeVersion: String? = null

  @set:Argument("major-ide-path", alias = "mip", description = "The path to the IDE with which to compare API problems")
  var majorIdePath: String? = null
}