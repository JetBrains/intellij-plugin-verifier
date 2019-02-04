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
import org.jetbrains.ide.diff.builder.api.*
import org.jetbrains.ide.diff.builder.persistence.ApiReportReader
import org.jetbrains.ide.diff.builder.persistence.saveTo
import org.jetbrains.ide.diff.builder.signatures.ApiSignature
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

    private fun getDiffsPath(resultsDirectory: Path) = resultsDirectory.resolve("diffs")

    private fun getIdeDiffPath(resultsDirectory: Path, oneIdeVersion: IdeVersion, twoIdeVersion: IdeVersion): Path =
        getDiffsPath(resultsDirectory).resolve("$oneIdeVersion-vs-$twoIdeVersion.zip")

    private fun getIdeAnnotationsResultPath(resultsDirectory: Path, ideVersion: IdeVersion): Path =
        resultsDirectory.resolve(getAnnotationsFileName(ideVersion))

    private fun getAnnotationsFileName(ideVersion: IdeVersion): String {
      val artifactId = IntelliJIdeRepository.getArtifactIdByProductCode(ideVersion.productCode)
      checkNotNull(artifactId) { ideVersion.asString() }
      return "$artifactId-${ideVersion.asStringWithoutProductCode()}-annotations.zip"
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

    val repositoryToIdes = allIdeRepositories.associate { ideRepository ->
      ideRepository to ideRepository.fetchIndex()
          .map { it.version }
          .filter { it.productCode == "IU" && it >= MIN_BUILD_NUMBER }
          .sorted()
    }
    val idesToProcess = repositoryToIdes.values.flatten().distinct().sorted()

    LOG.info("The following ${idesToProcess.size} IDEs (> $MIN_BUILD_NUMBER) are available in all IDE repositories: " + idesToProcess.joinToString())

    if (System.getProperty("ide.diff.builder.rebuild").orEmpty().equals("true", true)) {
      val diffsPath = getDiffsPath(resultsDirectory)
      LOG.info("Removing all existing IDE diffs from $diffsPath")
      diffsPath.deleteLogged()
    }

    check(idesToProcess.size > 1) { "Too few IDE builds to process: ${idesToProcess.size}" }

    LOG.info("Building IDE diffs for ${idesToProcess.size} adjacent IDEs: " + idesToProcess.joinToString())
    val ideDiffs = buildAdjacentIdeDiffs(idesToProcess, resultsDirectory, ideFilesBank, packages, jdkPath)

    LOG.info("Merging all IDE diffs into one accumulated report")
    var accumulatedReport: ApiReport = ApiReportReader.readFrom(ideDiffs.first().reportPath)
    for (ideDiff in ideDiffs.drop(1)) {
      val apiReport = ApiReportReader.readFrom(ideDiff.reportPath)
      accumulatedReport = ApiReportsMerger().mergeApiReports(ideDiff.newIdeVersion, listOf(accumulatedReport, apiReport))
    }

    val accumulatedPath = resultsDirectory.resolve("accumulated-up-to-${idesToProcess.last()}.zip")
    accumulatedReport.saveTo(accumulatedPath)
    LOG.info("The accumulated report has been built and saved to ${accumulatedPath.simpleName}.")

    LOG.info("Building annotations for last IDEs of each branch for each repository.")
    for (ideRepository in allIdeRepositories) {
      val branchToLastIdeVersion = repositoryToIdes.getValue(ideRepository)
          .groupingBy { it.baselineVersion }
          .reduce { _, acc, ideVersion -> maxOf(acc, ideVersion) }
      for ((branch, ideVersion) in branchToLastIdeVersion) {
        LOG.info("Building annotations for last IDE of branch $branch: $ideVersion from $ideRepository")
        val resultPath = getIdeAnnotationsResultPath(resultsDirectory, ideVersion)
        val annotations = buildApiAnnotations(accumulatedReport, ideVersion)
        annotations.saveTo(resultPath)
      }
    }
  }

  private data class IdeDiff(val reportPath: Path, val oldIdeVersion: IdeVersion, val newIdeVersion: IdeVersion)

  private fun buildAdjacentIdeDiffs(
      idesToProcess: List<IdeVersion>,
      resultsDirectory: Path,
      ideFilesBank: IdeFilesBank,
      packages: List<String>,
      jdkPath: JdkPath
  ): List<IdeDiff> {
    val ideDiffs = arrayListOf<IdeDiff>()
    for (index in 1 until idesToProcess.size) {
      val previousIdeVersion = idesToProcess[index - 1]
      val currentIdeVersion = idesToProcess[index]

      LOG.info("____Building IDE diff ($index of ${idesToProcess.size - 1}) between $previousIdeVersion and $currentIdeVersion")

      val ideDiffPath = getIdeDiffPath(resultsDirectory, previousIdeVersion, currentIdeVersion)
      if (ideDiffPath.exists()) {
        LOG.info("________IDE diff between $previousIdeVersion and $currentIdeVersion is already built")
      } else {
        val apiReport = buildIdeDiffBetweenIdes(previousIdeVersion, currentIdeVersion, ideFilesBank, packages, jdkPath)
        LOG.info("________Saving IDE diff between $previousIdeVersion and $currentIdeVersion to $ideDiffPath")
        apiReport.saveTo(ideDiffPath)
      }
      ideDiffs += IdeDiff(ideDiffPath, previousIdeVersion, currentIdeVersion)
    }
    return ideDiffs
  }

  private fun buildApiAnnotations(mergedReport: ApiReport, ideVersion: IdeVersion): ApiReport {
    val apiSignatureToEvents = hashMapOf<ApiSignature, Set<ApiEvent>>()

    val allSignatures = mergedReport.asSequence().map { it.first }.distinct()
    for (signature in allSignatures) {
      val events = mergedReport[signature]
      if (events.isEmpty()) {
        continue
      }
      val sanitizedEvents = arrayListOf<ApiEvent>()

      val firstEvent = events.first()
      if (firstEvent is IntroducedIn) {
        if (firstEvent.ideVersion.baselineVersion > ideVersion.baselineVersion) {
          //Too new signature for this IDE => skip
          continue
        }
        sanitizedEvents += firstEvent
      }

      val lastEvent = events.last()
      if (lastEvent is RemovedIn) {
        if (lastEvent.ideVersion.baselineVersion < ideVersion.baselineVersion) {
          //Too old signature for this IDE => skip
          continue
        }
        sanitizedEvents += lastEvent
      }

      apiSignatureToEvents[signature] = sanitizedEvents.toSet()
    }
    return ApiReport(ideVersion, apiSignatureToEvents)
  }

  private fun createIdeFilesBank(idesDir: Path): IdeFilesBank {
    val gigabytes = System.getProperty("ides.dir.max.size.gb", "10").toInt()
    val diskSpaceSetting = DiskSpaceSetting(SpaceAmount.ONE_GIGO_BYTE * gigabytes)
    return IdeFilesBank(idesDir, allIdeMergingRepository, diskSpaceSetting)
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

}