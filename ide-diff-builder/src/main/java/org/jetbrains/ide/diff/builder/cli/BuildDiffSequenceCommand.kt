package org.jetbrains.ide.diff.builder.cli

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Builds a cumulative sequence of API diffs of multiple IDE builds.
 *
 * @see [help]
 */
class BuildDiffSequenceCommand : Command {

  private companion object {
    private val LOG = LoggerFactory.getLogger("build-diff-sequence")
  }

  override val help
    get() = """
      Builds a cumulative sequence of API diffs of multiple IDE builds.
      API diffs are saved in .zip files under the /result directory.

      build-diff-sequence [-ides-dir <path>] [-packages <packages>] path/to/result path/to/IDE-1 [path/to/IDE-2 ... path/to/IDE-n]

      -ides-dir option is used to specify a path where downloaded IDE builds will be kept.
      If not specified, temp directory will be used.

      -packages <packages> is semicolon (';') separated list of packages to be processed.
      See -help of 'ide-diff' command for more info.

      For example:
      java -jar diff-builder.jar build-diff-sequence -ides-dir ./ides-cache result/ IU-181.1 IU-181.9 IU-182.1 IU-183.1

      will build API diffs between
       IU-181.1 <-> IU-181.9  ---> result/IU-api-since-IU-181.9-annotations.zip
       IU-181.9 <-> IU-182.1  ---> result/IU-api-since-IU-182.1-annotations.zip (contains since IU-181.9)
       IU-182.1 <-> IU-183.1  ---> result/IU-api-since-IU-183.1-annotations.zip (contains since IU-182.1)

      Downloaded IDE builds will be cached in ./ides-cache, which is limited in size to 10 GB.

      The resulting diffs will be saved to result/ directory in files <version>.zip,
      which contains cumulative "available since" external annotations for all IDEs
      prior to that version in the versions sequence.
    """.trimIndent()

  override val commandName
    get() = "build-diff-sequence"

  open class CliOptions : IdeDiffCommand.CliOptions() {
    @set:Argument("ides-dir", description = "Path where downloaded IDE builds are cached")
    var idesDirPath: String? = null

    fun getIdePath(): Path =
        if (idesDirPath != null) {
          Paths.get(idesDirPath)
        } else {
          Files.createTempDirectory("ides-dir").also {
            it.toFile().deleteOnExit()
          }
        }
  }

  private data class Options(
      val idesDir: Path,
      val resultPath: Path,
      val ideVersions: List<IdeVersion>,
      val packages: List<String>
  )

  /**
   * Parses command line options of the `build-diff-sequence` command.
   */
  private fun parseOptions(freeArgs: List<String>): Options {
    val cliOptions = CliOptions()
    var args = Args.parse(cliOptions, freeArgs.toTypedArray(), true)

    val idesDir = cliOptions.getIdePath()

    val resultPath = Paths.get(args.first())
    resultPath.deleteLogged()

    args = args.drop(1)

    if (args.size < 2) {
      System.err.println("At least 2 IDEs must be specified")
      exit(help)
    }

    val ideVersions = args.map { IdeVersion.createIdeVersion(it) }
    checkIdeVersionsAvailable(ideVersions)
    return Options(idesDir, resultPath, ideVersions, cliOptions.packages.toList())
  }

  private fun checkIdeVersionsAvailable(ideVersions: List<IdeVersion>) {
    for (ideVersion in ideVersions) {
      if (allIdeRepository.fetchAvailableIde(ideVersion) == null) {
        throw IllegalArgumentException("IDE $ideVersion is not available in the IntelliJ artifacts repositories\n" +
            "Only the following IDEs are available: " + allIdeRepository.fetchIndex().joinToString())
      }
    }
  }

  override fun execute(freeArgs: List<String>) {
    val (idesDir, resultPath, ideVersions, packages) = parseOptions(freeArgs)
    val ideFilesBank = IdeFilesBank(
        idesDir,
        allIdeRepository,
        DiskSpaceSetting(SpaceAmount.ONE_GIGO_BYTE * 10)
    )

    LOG.info("Building API diffs for a list of IDE builds: " + ideVersions.joinToString())
    buildDiffs(ideVersions, ideFilesBank, resultPath, packages)
  }

  /**
   * Builds diffs between adjacent IDE builds listed in [ideVersions]
   * and saves cumulative "available since" external annotations roots under [resultsDirectory].
   */
  private fun buildDiffs(
      ideVersions: List<IdeVersion>,
      ideFilesBank: IdeFilesBank,
      resultsDirectory: Path,
      packages: List<String>
  ) {
    var previousResult: Path? = null
    for (i in 0 until ideVersions.size - 1) {
      val oldIdeVersion = ideVersions[i]
      val newIdeVersion = ideVersions[i + 1]

      val resultPath = BuildMissingSinceAnnotationsCommand.getIdeAnnotationsResultPath(resultsDirectory, newIdeVersion)
      resultPath.deleteLogged()

      IdeDiffCommand().buildIdeDiff(oldIdeVersion, newIdeVersion, ideFilesBank, packages, resultPath)
      if (previousResult != null) {
        MergeSinceDataCommand().mergeSinceData(previousResult, resultPath, resultPath)
      }

      previousResult = resultPath
      LOG.info("Cumulative API diff between $newIdeVersion and previous IDE builds has been saved to $resultPath")
    }
  }

}
