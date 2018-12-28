package org.jetbrains.ide.diff.builder.cli

import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.misc.retry
import com.jetbrains.pluginverifier.misc.simpleName
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import org.jetbrains.ide.diff.builder.api.SinceApiBuilder
import org.jetbrains.ide.diff.builder.persistence.SinceApiWriter
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Builds API diff between two IDE versions,
 * and saves the result as external "available since"
 * annotations root with since-build equal to the second IDE.
 * @see [help]
 */
class IdeDiffCommand : Command {
  companion object {
    private val LOG = LoggerFactory.getLogger("ide-diff")
  }

  override val commandName
    get() = "ide-diff"

  override val help
    get() = """
      Builds API diff between two IDE versions, and saves the result as
      external "available since <second IDE version>" annotations root.

      ide-diff [-packages <packages>] <old IDE path> <new IDE path> <result path>

      -packages <packages> is semicolon (';') separated list of packages to be processed.
      By default it's equal to "org.jetbrains;com.jetbrains;org.intellij;com.intellij".
      If an empty package is specified using "", all packages will be processed.

      For example:
      java -jar diff-builder.jar ide-diff path/to/IU-183.1 path/to/IU-183.567 path/to/result

      will build and save "available since" IU-183.567 external annotations
      to path/to/result, which can be a directory or a zip file.
    """.trimIndent()

  override fun execute(freeArgs: List<String>) {
    val cliOptions = CliOptions()
    val args = Args.parse(cliOptions, freeArgs.toTypedArray(), false)
    if (args.size < 3) {
      exit("Paths to <old IDE> <new IDE> <result> must be specified.")
    }

    val oldIdePath = Paths.get(args[0])
    val newIdePath = Paths.get(args[1])
    val resultRoot = Paths.get(args[2])
    val packages = cliOptions.getPackages()
    LOG.info(if (packages.any { it.isEmpty() }) {
      "All packages will be processed"
    } else {
      "The following packages will be processed: " + packages.joinToString()
    })

    buildIdeDiff(oldIdePath, newIdePath, resultRoot, packages)
  }

  private fun buildIdeDiff(
      oldIdePath: Path,
      newIdePath: Path,
      resultRoot: Path,
      packages: List<String>
  ) {
    val oldIde = IdeManager.createManager().createIde(oldIdePath.toFile())
    val newIde = IdeManager.createManager().createIde(newIdePath.toFile())
    LOG.info("Building API diff between ${oldIde.version} and ${newIde.version}")

    val sinceApiData = SinceApiBuilder(packages).build(oldIde, newIde)
    SinceApiWriter(resultRoot, sinceApiData.ideBuildNumber).use {
      it.appendSinceApiData(sinceApiData)
    }
    LOG.info("New API in ${newIde.version} compared to ${oldIde.version} is saved to external annotations root ${resultRoot.simpleName}")
  }

  fun buildIdeDiff(
      oldIdeVersion: IdeVersion,
      newIdeVersion: IdeVersion,
      ideFilesBank: IdeFilesBank,
      packages: List<String>,
      resultPath: Path
  ) {
    val oldIdeResult = ideFilesBank.downloadIde(oldIdeVersion)
    return oldIdeResult.ideFileLock.use { oldIdeFileLock ->
      val newIdeResult = ideFilesBank.downloadIde(newIdeVersion)
      newIdeResult.ideFileLock.use { newIdeFileLock ->
        buildIdeDiff(
            oldIdeFileLock.file,
            newIdeFileLock.file,
            resultPath,
            packages
        )
      }
    }
  }

  private fun IdeFilesBank.downloadIde(ideVersion: IdeVersion): IdeFilesBank.Result.Found {
    val message = "Downloading $ideVersion"
    return retry(message) {
      LOG.info(message)
      val ideFile = getIdeFile(ideVersion)
      when (ideFile) {
        is IdeFilesBank.Result.Found -> ideFile
        is IdeFilesBank.Result.NotFound -> throw IllegalArgumentException("$ideVersion is not found: ${ideFile.reason}")
        is IdeFilesBank.Result.Failed -> throw IllegalArgumentException("$ideVersion couldn't be downloaded: ${ideFile.reason}", ideFile.exception)
      }
    }
  }

  open class CliOptions {
    @set:Argument("packages", delimiter = ";", description = "Semicolon (';') separated list of packages to be processed. " +
        "By default it is equal to \"org.jetbrains;com.jetbrains;org.intellij;com.intellij\". " +
        "If an empty package is specified using \"\", all packages will be processed.")
    var packages: Array<String> = arrayOf("org.jetbrains", "com.jetbrains", "org.intellij", "com.intellij")

    fun getPackages(): List<String> = packages.toList()
  }

}