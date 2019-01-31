package org.jetbrains.ide.diff.builder.cli

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.ide.IntelliJIdeRepository
import com.jetbrains.pluginverifier.misc.*
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.files.FileLock
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import org.jetbrains.ide.diff.builder.api.ApiReport
import org.jetbrains.ide.diff.builder.api.ApiReportsMerger
import org.jetbrains.ide.diff.builder.api.IdeDiffBuilder
import org.jetbrains.ide.diff.builder.maven.downloadArtifactTo
import org.jetbrains.ide.diff.builder.persistence.ApiReportReader
import org.jetbrains.ide.diff.builder.persistence.saveTo
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Builds API annotations artifacts for IDEs from the IntelliJ Artifacts Repositories
 * and saves them under results directory with names like `ideaIU-191.1234-annotations.zip`.
 */
class BuildApiAnnotationsCommand : Command {

  companion object {
    private val LOG = LoggerFactory.getLogger("build-api-annotations")

    private val MIN_BUILD_NUMBER = IdeVersion.createIdeVersion("171.1")

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

    private fun mustRebuildAllAnnotations(): Boolean =
        System.getProperty("ide.diff.builder.rebuild.all.annotations").orEmpty().equals("true", true)

    /**
     * Returns previous branch number for specified branch.
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

    private val ioExecutor = Executors.newCachedThreadPool(
        ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("io-%d")
            .build()
    )
  }

  override val commandName: String
    get() = "build-api-annotations"

  override val help: String
    get() = """
      Builds API annotations artifacts for IDEs that lack such annotations in the IntelliJ Artifacts Repositories:
      https://www.jetbrains.com/intellij-repository/releases/ and https://www.jetbrains.com/intellij-repository/snapshots
      It saves them under results directory with names like `ideaIU-191.1234-annotations.zip`.

      build-api-annotations [-ides-dir <IDE cache dir] [-jdk-path <path to JDK home>] [-packages "org.some;com.another"] <results directory>
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
    resultsDirectory.createDir()
    LOG.info("Results will be saved to $resultsDirectory")

    val jdkPath = cliOptions.getJdkPath()
    LOG.info("JDK will be used to resolve java classes: $jdkPath")

    val packages = cliOptions.getPackages()
    LOG.info(if (packages.any { it.isEmpty() }) {
      "All packages will be processed"
    } else {
      "The following packages will be processed: " + packages.joinToString()
    })

    val idesDir = cliOptions.getIdesDirectory()
    LOG.info("IDE cache directory to use: $idesDir")

    val ideFilesBank = createIdeFilesBank(idesDir)

    val availableIdes = allIdeRepository.fetchIndex().asSequence()
        .map { it.version }
        .filter { it.productCode == "IU" }
        .toList().sorted()
    LOG.info("The following ${availableIdes.size} IDEs are available in $INTELLIJ_ARTIFACTS_REPOSITORY_NAME: " + availableIdes.joinToString())

    val alreadyProcessedIdes = if (mustRebuildAllAnnotations()) {
      emptyList()
    } else {
      availableIdes.filter { getIdeAnnotationsResultPath(resultsDirectory, it).exists() }
    }

    LOG.info("The following ${alreadyProcessedIdes.size} IDEs are already processed locally: " + alreadyProcessedIdes.joinToString())

    val idesToProcess = availableIdes.filter { ideVersion ->
      ideVersion >= MIN_BUILD_NUMBER && ideVersion !in alreadyProcessedIdes
    }

    LOG.info("Building annotations for the following ${idesToProcess.size} IDEs: " + idesToProcess.joinToString())

    for ((index, currentIdeVersion) in idesToProcess.withIndex()) {
      LOG.info("Building annotations for $currentIdeVersion (${index + 1} of ${idesToProcess.size})")

      val baseIdeVersion = selectBaseIdeVersion(currentIdeVersion, availableIdes)
      val lastIdeVersion = selectLastIdeVersion(currentIdeVersion, availableIdes)
      LOG.info("____base IDE = $baseIdeVersion; last IDE = $lastIdeVersion")

      LOG.info("____Building API diff between base=$baseIdeVersion and current=$currentIdeVersion")
      val baseDiff = buildIdeDiffBetweenIdes(baseIdeVersion, currentIdeVersion, ideFilesBank, packages, jdkPath)

      LOG.info("____Building API diff between last=$lastIdeVersion and current=$currentIdeVersion")
      val lastDiff = buildIdeDiffBetweenIdes(lastIdeVersion, currentIdeVersion, ideFilesBank, packages, jdkPath)

      LOG.info("____Searching for result for last=$lastIdeVersion")
      val lastResult = getOrDownloadApiReport(lastIdeVersion, resultsDirectory)

      val reports = listOfNotNull(baseDiff, lastDiff, lastResult)
      LOG.info("____Merging results for " + reports.joinToString { it.ideBuildNumber.asString() })
      val mergedApiReport = ApiReportsMerger().mergeApiReports(currentIdeVersion, reports)

      val resultPath = getIdeAnnotationsResultPath(resultsDirectory, currentIdeVersion)
      resultPath.deleteLogged()

      LOG.info("____Result for $currentIdeVersion has been saved to ${resultPath.simpleName}")
      mergedApiReport.saveTo(resultPath)
    }
  }

  private fun createIdeFilesBank(idesDir: Path): IdeFilesBank {
    val gigabytes = System.getProperty("ides.dir.max.size.gb", "10").toInt()
    val diskSpaceSetting = DiskSpaceSetting(SpaceAmount.ONE_GIGO_BYTE * gigabytes)
    return IdeFilesBank(idesDir, allIdeRepository, diskSpaceSetting)
  }

  private fun buildIdeDiffBetweenIdes(
      oldIdeVersion: IdeVersion,
      newIdeVersion: IdeVersion,
      ideFilesBank: IdeFilesBank,
      packages: List<String>,
      jdkPath: JdkPath
  ): ApiReport {
    val oldIdeTask = ideFilesBank.downloadIdeAsync(oldIdeVersion)
    val newIdeTask = ideFilesBank.downloadIdeAsync(newIdeVersion)

    val (oldIdeFile, oldException) = oldIdeTask.getOrException()
    val (newIdeFile, newException) = newIdeTask.getOrException()

    try {
      if (oldException != null) throw oldException
      if (newException != null) throw newException
      LOG.info("________Both IDEs have been downloaded, building their diff")
      return IdeDiffBuilder(packages, jdkPath).buildIdeDiff(oldIdeFile!!.file, newIdeFile!!.file)
    } finally {
      oldIdeFile.closeLogged()
      newIdeFile.closeLogged()
    }
  }

  private fun <T> Future<T>.getOrException(): Pair<T?, Throwable?> {
    return try {
      get() to null
    } catch (e: ExecutionException) {
      null to e.cause!!
    } catch (e: Throwable) {
      null to e
    }
  }

  private fun IdeFilesBank.downloadIdeAsync(ideVersion: IdeVersion): Future<FileLock> =
      ioExecutor.submit<FileLock> { downloadIde(ideVersion) }

  private fun IdeFilesBank.downloadIde(ideVersion: IdeVersion): FileLock {
    val message = "________Downloading $ideVersion"
    LOG.info(message)
    return retry(message) {
      val ideFile = getIdeFile(ideVersion)
      when (ideFile) {
        is IdeFilesBank.Result.Found -> ideFile.ideFileLock
        is IdeFilesBank.Result.NotFound -> throw IllegalArgumentException("$ideVersion is not found: ${ideFile.reason}")
        is IdeFilesBank.Result.Failed -> throw IllegalArgumentException("$ideVersion couldn't be downloaded: ${ideFile.reason}", ideFile.exception)
      }
    }
  }

  private fun getOrDownloadApiReport(ideVersion: IdeVersion, resultsDirectory: Path): ApiReport? {
    val resultPath = getIdeAnnotationsResultPath(resultsDirectory, ideVersion)
    if (resultPath.exists()) {
      LOG.info("________Result for $ideVersion is available at ${resultPath.simpleName}")
      return ApiReportReader.readFrom(resultPath)
    }
    if (ideVersion < MIN_BUILD_NUMBER) {
      //We don't build annotations for old IDEs.
      return null
    }
    try {
      val message = "________Downloading annotations for $ideVersion to ${resultPath.simpleName}"
      retry(message) {
        downloadAnnotations(ideVersion, resultPath)
      }
    } catch (e: Exception) {
      throw IllegalStateException("Annotations for $ideVersion were not found locally, nor they could be downloaded from the repository.", e)
    }
    return ApiReportReader.readFrom(resultPath)
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
   * Selects latest IDE build from the previous branch.
   */
  private fun selectBaseIdeVersion(ideVersion: IdeVersion, ideIndex: List<IdeVersion>): IdeVersion {
    val previousBranch = getPreviousBranch(ideVersion.baselineVersion)
    return ideIndex.filter { it.baselineVersion == previousBranch }.max()
        ?: throw RuntimeException("For $ideVersion there is no IDE in the previous branch")
  }

  /**
   * Selects IDE build previous for the current one.
   */
  private fun selectLastIdeVersion(ideVersion: IdeVersion, ideIndex: List<IdeVersion>): IdeVersion =
      ideIndex.filter { it < ideVersion }.max()
          ?: throw RuntimeException("For $ideVersion there is no previous IDE")

}