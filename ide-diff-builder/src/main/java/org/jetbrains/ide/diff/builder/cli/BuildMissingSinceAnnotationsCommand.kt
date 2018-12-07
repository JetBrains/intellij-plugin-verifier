package org.jetbrains.ide.diff.builder.cli

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.misc.exists
import com.jetbrains.pluginverifier.misc.retry
import com.jetbrains.pluginverifier.misc.simpleName
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.sampullara.cli.Args
import org.apache.commons.io.FileUtils
import org.jetbrains.ide.diff.builder.maven.buildMavenDownloadUrl
import org.jetbrains.ide.diff.builder.maven.requestMavenAvailableVersions
import org.slf4j.LoggerFactory
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

/**
 * Builds "API is available since" artifacts for IDEs that lack such annotations in the IntelliJ Artifacts Repositories.
 * It saves them under results directory with names like `IU-api-since-191.1234-annotations.zip`.
 */
class BuildMissingSinceAnnotationsCommand : Command {

  companion object {
    private val LOG = LoggerFactory.getLogger("build-missing-since-annotations")

    private val MIN_BUILD_NUMBER = IdeVersion.createIdeVersion("181.1")

    private val URL_TIMEOUT = TimeUnit.MINUTES.toMillis(1).toInt()

    const val INTELLIJ_ARTIFACTS_REPOSITORY_NAME = "IntelliJ Artifact Repository"
    const val INTELLIJ_ARTIFACTS_REPOSITORY_BASE_URL = "https://cache-redirector.jetbrains.com/intellij-repository/"
    const val INTELLIJ_ARTIFACTS_REPOSITORY_RELEASES_URL = "$INTELLIJ_ARTIFACTS_REPOSITORY_BASE_URL/releases"
    const val INTELLIJ_ARTIFACTS_REPOSITORY_SNAPSHOTS_URL = "$INTELLIJ_ARTIFACTS_REPOSITORY_BASE_URL/snapshots"

    const val ANNOTATIONS_GROUP_ID = "com.jetbrains.intellij.idea"
    const val ANNOTATIONS_ARTIFACT_ID = "IU-api-since"
    const val ANNOTATIONS_CLASSIFIER = "annotations"
    const val ANNOTATIONS_PACKAGING = "zip"

    private fun getAnnotationsFileName(ideVersion: IdeVersion) =
        ANNOTATIONS_ARTIFACT_ID + '-' + ideVersion.asStringWithoutProductCode() + '-' + ANNOTATIONS_CLASSIFIER + '.' + ANNOTATIONS_PACKAGING

    fun getIdeAnnotationsResultPath(resultsDirectory: Path, ideVersion: IdeVersion): Path =
        resultsDirectory.resolve(getAnnotationsFileName(ideVersion))
  }

  override val commandName: String
    get() = "build-missing-since-annotations"

  override val help: String
    get() = """
      Builds "API available since" artifacts for IDEs that lack such annotations in the IntelliJ Artifacts Repositories:
      https://www.jetbrains.com/intellij-repository/releases/ and https://www.jetbrains.com/intellij-repository/snapshots
      It saves them under results directory with names like `IU-api-since-191.1234-annotations.zip`.

      build-missing-since-annotations [-ides-dir <IDE cache dir] [-packages "org.some;com.another"] <results directory>
    """.trimIndent()

  override fun execute(freeArgs: List<String>) {
    val cliOptions = BuildDiffSequenceCommand.CliOptions()
    val args = Args.parse(cliOptions, freeArgs.toTypedArray(), false)

    val resultsDirectory = Paths.get(args[0])
    val packages = cliOptions.getPackages()
    LOG.info(if (packages.any { it.isEmpty() }) {
      "All packages will be processed"
    } else {
      "The following packages will be processed: " + packages.joinToString()
    })

    val idePath = cliOptions.getIdePath()
    LOG.info("IDE cache directory to use: $idePath")

    val ideFilesBank = IdeFilesBank(idePath, allIdeRepository, DiskSpaceSetting(SpaceAmount.ONE_GIGO_BYTE * 10))

    val releasesAnnotations = requestAvailableAnnotationsVersions(INTELLIJ_ARTIFACTS_REPOSITORY_RELEASES_URL)
    val snapshotsAnnotations = requestAvailableAnnotationsVersions(INTELLIJ_ARTIFACTS_REPOSITORY_SNAPSHOTS_URL)

    val availableIdes = allIdeRepository.fetchIndex().asSequence()
        .map { it.version }
        .filter { it.productCode == "IU" }
        .toList().sorted()
    LOG.info("The following ${availableIdes.size} IDEs are available in $INTELLIJ_ARTIFACTS_REPOSITORY_NAME: " + availableIdes.joinToString())

    val idesWithoutAnnotations = availableIdes
        .filter { version ->
          version >= MIN_BUILD_NUMBER
              && version.asStringWithoutProductCode() !in releasesAnnotations
              && version.asStringWithoutProductCode() !in snapshotsAnnotations
        }

    LOG.info("The following ${idesWithoutAnnotations.size} IDEs lack 'API available since' annotations: " + idesWithoutAnnotations.joinToString())

    for (ideVersion in idesWithoutAnnotations) {
      val resultPath = getIdeAnnotationsResultPath(resultsDirectory, ideVersion)

      val baseIdeVersion = selectBaseIdeVersion(ideVersion, availableIdes) ?: return
      IdeDiffCommand().buildIdeDiff(baseIdeVersion, ideVersion, ideFilesBank, packages, resultPath)

      val previousIdeVersion = availableIdes.filter { it < ideVersion }.max()
          ?: throw RuntimeException("For $ideVersion there is no previous IDE")
      val previousAnnotations = getOrDownloadSinceAnnotations(previousIdeVersion, resultsDirectory)

      if (previousAnnotations != null) {
        MergeSinceDataCommand().mergeSinceData(previousAnnotations, resultPath, resultPath)
      }
    }
  }

  private fun requestAvailableAnnotationsVersions(repoUrl: String): List<String> {
    //Todo: remove this option when the annotations are uploaded to the repository.
    if (System.getProperty("before.annotations.uploaded") != null) {
      return emptyList()
    }
    return retry("Request available annotations versions from $repoUrl") {
      requestMavenAvailableVersions(repoUrl, ANNOTATIONS_GROUP_ID, ANNOTATIONS_ARTIFACT_ID)
    }
  }

  private fun getOrDownloadSinceAnnotations(ideVersion: IdeVersion, resultsDirectory: Path): Path? {
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
    val message = "Downloading annotations for $ideVersion to ${resultPath.simpleName}"
    LOG.info(message)
    retry(message) {
      downloadAnnotations(ideVersion, resultPath)
    }
    return resultPath
  }

  private fun downloadAnnotations(ideVersion: IdeVersion, resultPath: Path) {
    val downloadUrl = URL(buildMavenDownloadUrl(
        INTELLIJ_ARTIFACTS_REPOSITORY_BASE_URL,
        ANNOTATIONS_GROUP_ID,
        ANNOTATIONS_ARTIFACT_ID,
        ideVersion.asStringWithoutProductCode(),
        ANNOTATIONS_CLASSIFIER,
        ANNOTATIONS_PACKAGING
    ))
    FileUtils.copyURLToFile(downloadUrl, resultPath.toFile(), URL_TIMEOUT, URL_TIMEOUT)
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