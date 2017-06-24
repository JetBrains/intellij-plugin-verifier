package com.jetbrains.pluginverifier.configurations

import com.google.common.util.concurrent.AtomicDouble
import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.misc.extractTo
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.repository.IdeRepository
import com.jetbrains.pluginverifier.repository.RepositoryConfiguration
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files

/**
 * @author Sergey Patrikeev
 */
class CheckTrunkApiParamsParser : ConfigurationParamsParser<CheckTrunkApiParams> {

  companion object {

    private val LOG: Logger = LoggerFactory.getLogger(CheckTrunkApiParamsParser::class.java)

    private val IDE_DOWNLOAD_ATTEMPTS = 3
  }

  override fun parse(opts: CmdOpts, freeArgs: List<String>): CheckTrunkApiParams {
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
      majorIdeFile = downloadIde(parseIdeVersion(apiOpts.majorIdeVersion!!))
      deleteMajorOnExit = true
    } else {
      throw IllegalArgumentException("Neither the version (-miv) nor the path to the IDE (-mip) with which to compare API problems specified")
    }

    val externalClassesPrefixes = OptionsParser.getExternalClassesPrefixes(opts)
    val problemsFilter = OptionsParser.getProblemsFilter(opts)

    val majorIdeDescriptor = OptionsParser.createIdeDescriptor(majorIdeFile, opts)
    return CheckTrunkApiParams(ideDescriptor, majorIdeDescriptor, externalClassesPrefixes, problemsFilter, jdkDescriptor, deleteMajorOnExit, majorIdeFile)
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
    val ideZip = tryDownloadIde(ideVersion)
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

  private fun tryDownloadIde(ideVersion: IdeVersion): File {
    for (attempt in 1..IDE_DOWNLOAD_ATTEMPTS) {
      val ideZip = try {
        downloadIdeZip(ideVersion)
      } catch (e: Exception) {
        LOG.error("Attempt #$attempt to download IDE is failed", e)
        continue
      }
      return ideZip
    }
    throw RuntimeException("Unable to download IDE $ideVersion in $IDE_DOWNLOAD_ATTEMPTS attempts")
  }

  private fun downloadIdeZip(ideVersion: IdeVersion): File {
    val tempFile = File.createTempFile("ide", ".zip", RepositoryConfiguration.ideDownloadDir)

    val lastProgress = AtomicDouble()
    val progressUpdater: (Double) -> Unit = {
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


data class CheckTrunkApiParams(val trunkDescriptor: IdeDescriptor,
                               val releaseDescriptor: IdeDescriptor,
                               val externalClassesPrefixes: List<String>,
                               val problemsFilter: ProblemsFilter,
                               val jdkDescriptor: JdkDescriptor,
                               private val deleteMajorIdeOnExit: Boolean,
                               private val majorIdeFile: File,
                               val progress: Progress = DefaultProgress()) : ConfigurationParams {
  override fun presentableText(): String = """Check Trunk API Configuration Parameters:
Trunk IDE to be checked: $trunkDescriptor
Release IDE to compare API with: $releaseDescriptor
External classes prefixes: [${externalClassesPrefixes.joinToString()}]
JDK: $jdkDescriptor
"""

  override fun close() {
    trunkDescriptor.closeLogged()
    releaseDescriptor.closeLogged()
    if (deleteMajorIdeOnExit) {
      majorIdeFile.deleteLogged()
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