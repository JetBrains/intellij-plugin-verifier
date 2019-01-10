package org.jetbrains.ide.diff.builder.cli

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.ide.IntelliJIdeRepository
import com.jetbrains.pluginverifier.misc.exists
import com.jetbrains.pluginverifier.misc.retry
import com.jetbrains.pluginverifier.misc.simpleName
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import org.jetbrains.ide.diff.builder.maven.downloadArtifactTo
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Builds "API is available since" artifacts for IDEs that lack such annotations in the IntelliJ Artifacts Repositories.
 * It saves them under results directory with names like `ideaIU-191.1234-annotations.zip`.
 */
class BuildMissingSinceAnnotationsCommand : Command {

  companion object {
    private val LOG = LoggerFactory.getLogger("build-missing-since-annotations")

    private val MIN_BUILD_NUMBER = IdeVersion.createIdeVersion("181.1")

    const val INTELLIJ_ARTIFACTS_REPOSITORY_NAME = "IntelliJ Artifact Repository"
    const val INTELLIJ_ARTIFACTS_REPOSITORY_BASE_URL = "https://cache-redirector.jetbrains.com/intellij-repository"

    const val ANNOTATIONS_CLASSIFIER = "annotations"
    const val ANNOTATIONS_PACKAGING = "zip"

    fun getIdeAnnotationsResultPath(resultsDirectory: Path, ideVersion: IdeVersion): Path =
        resultsDirectory.resolve(getAnnotationsFileName(ideVersion))

    private fun getAnnotationsFileName(ideVersion: IdeVersion): String {
      val artifactId = IntelliJIdeRepository.getArtifactIdByProductCode(ideVersion.productCode)
      checkNotNull(artifactId) { ideVersion.asString() }
      return artifactId + '-' + ideVersion.asStringWithoutProductCode() + '-' + ANNOTATIONS_CLASSIFIER + '.' + ANNOTATIONS_PACKAGING
    }
  }

  override val commandName: String
    get() = "build-missing-since-annotations"

  override val help: String
    get() = """
      Builds "API available since" artifacts for IDEs that lack such annotations in the IntelliJ Artifacts Repositories:
      https://www.jetbrains.com/intellij-repository/releases/ and https://www.jetbrains.com/intellij-repository/snapshots
      It saves them under results directory with names like `ideaIU-191.1234-annotations.zip`.

      build-missing-since-annotations [-ides-dir <IDE cache dir] [-jdk-path <path to JDK home>] [-packages "org.some;com.another"] <results directory>
    """.trimIndent()

  open class CliOptions : IdeDiffCommand.CliOptions() {
    @set:Argument("ides-dir", description = "Path where downloaded IDE builds are cached")
    var idesDirPath: String? = null

    fun getIdesDirectory(): Path =
        if (idesDirPath != null) {
          Paths.get(idesDirPath)
        } else {
          Files.createTempDirectory("ides-dir").also {
            it.toFile().deleteOnExit()
          }
        }
  }

  override fun execute(freeArgs: List<String>) {
    val cliOptions = CliOptions()
    val args = Args.parse(cliOptions, freeArgs.toTypedArray(), false)

    val resultsDirectory = Paths.get(args[0])
    val packages = cliOptions.getPackages()
    val jdkPath = cliOptions.getJdkPath()
    LOG.info(if (packages.any { it.isEmpty() }) {
      "All packages will be processed"
    } else {
      "The following packages will be processed: " + packages.joinToString()
    })

    val idesDir = cliOptions.getIdesDirectory()
    LOG.info("IDE cache directory to use: $idesDir")

    val ideFilesBank = IdeFilesBank(idesDir, allIdeRepository, DiskSpaceSetting(SpaceAmount.ONE_GIGO_BYTE * 10))

    val availableIdes = allIdeRepository.fetchIndex().asSequence()
        .map { it.version }
        .filter { it.productCode == "IU" }
        .toList().sorted()
    LOG.info("The following ${availableIdes.size} IDEs are available in $INTELLIJ_ARTIFACTS_REPOSITORY_NAME: " + availableIdes.joinToString())

    val alreadyBuiltAnnotations = availableIdes.filter { getIdeAnnotationsResultPath(resultsDirectory, it).exists() }

    val idesToProcess = availableIdes.filter { it >= MIN_BUILD_NUMBER && it !in alreadyBuiltAnnotations }

    LOG.info("Building annotations for the following ${idesToProcess.size} IDEs: " + idesToProcess.joinToString())

    for (currentIdeVersion in idesToProcess) {
      val resultPath = getIdeAnnotationsResultPath(resultsDirectory, currentIdeVersion)

      val baseIdeVersion = selectBaseIdeVersion(currentIdeVersion, availableIdes) ?: return
      IdeDiffCommand().buildIdeDiff(baseIdeVersion, currentIdeVersion, ideFilesBank, packages, resultPath, jdkPath)

      val previousIdeVersion = availableIdes.filter { it < currentIdeVersion }.max()
          ?: throw RuntimeException("For $currentIdeVersion there is no previous IDE")
      val previousAnnotations = getOrDownloadAnnotations(previousIdeVersion, resultsDirectory)

      if (previousAnnotations != null) {
        MergeSinceDataCommand().mergeSinceData(previousAnnotations, resultPath, resultPath)
      }
    }
  }

  private fun getOrDownloadAnnotations(ideVersion: IdeVersion, resultsDirectory: Path): Path? {
    val resultPath = getIdeAnnotationsResultPath(resultsDirectory, ideVersion)
    if (resultPath.exists()) {
      return resultPath
    }
    if (ideVersion < MIN_BUILD_NUMBER) {
      /**
       * We don't build "available since" annotations for IDE builds prior to [MIN_BUILD_NUMBER],
       * so consider API prior to that builds has always existed.
       */
      return null
    }
    if (System.getProperty("ide.diff.builder.no.download.annotations").orEmpty().equals("true", true)) {
      return null
    }
    val message = "Downloading annotations for $ideVersion to ${resultPath.simpleName}"
    LOG.info(message)
    retry(message) {
      downloadAnnotations(ideVersion, resultPath)
    }
    return resultPath
  }

  private fun downloadAnnotations(ideVersion: IdeVersion, resultPath: Path) {
    val artifactId = IntelliJIdeRepository.getArtifactIdByProductCode(ideVersion.productCode)!!
    val groupId = IntelliJIdeRepository.getGroupIdByProductCode(ideVersion.productCode)!!
    val version = ideVersion.asStringWithoutProductCode()
    try {
      downloadArtifactTo("$INTELLIJ_ARTIFACTS_REPOSITORY_BASE_URL/releases", groupId, artifactId, version, resultPath, ANNOTATIONS_CLASSIFIER, ANNOTATIONS_PACKAGING)
    } catch (e: Exception) {
      LOG.info("Couldn't download annotations from /releases: ${e.message}. Searching in /snapshots")
      downloadArtifactTo("$INTELLIJ_ARTIFACTS_REPOSITORY_BASE_URL/snapshots", groupId, artifactId, version, resultPath, ANNOTATIONS_CLASSIFIER, ANNOTATIONS_PACKAGING)
    }
  }

  /**
   * Among all available IDE versions selects the latest from the previous branch.
   */
  private fun selectBaseIdeVersion(ideVersion: IdeVersion, ideIndex: List<IdeVersion>): IdeVersion? =
      ideIndex.filter { it.baselineVersion == getPreviousBranch(ideVersion.baselineVersion) }.max()
          ?: throw RuntimeException("For $ideVersion there is no IDE in the previous branch")
}

/**
 * Returns branch number previous for the current:
 * ```
 * 145 -> 144
 * 162 -> 145
 * 163 -> 162
 * 171 -> 163
 * ...
 * 181 -> 173
 * 182 -> 181
 * 183 -> 182
 * and so on ...
 * ```
 */
private fun getPreviousBranch(branch: Int): Int {
  check(branch <= 162 || branch % 10 <= 3) { "$branch" }
  if (branch == 162) {
    return 145
  }
  if (branch >= 163 && branch % 10 == 1) {
    return ((branch / 10) - 1) * 10 + 3
  }
  return branch - 1
}