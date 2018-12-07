package org.jetbrains.ide.diff.builder.cli

import com.jetbrains.pluginverifier.misc.simpleName
import org.jetbrains.ide.diff.builder.persistence.SinceApiReader
import org.jetbrains.ide.diff.builder.persistence.SinceApiWriter
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * Merges two "available since" external annotations roots.
 *
 * If the second root contains signatures present in the first root,
 * the corresponding "available since" versions are taken from the first one,
 * and the second is ignored.
 * @see [help]
 */
class MergeSinceDataCommand : Command {

  private companion object {
    private val LOG = LoggerFactory.getLogger("merge-since-data")
  }

  override val commandName
    get() = "merge-since-data"

  override val help
    get() = """
      Merges two "available since" external annotations roots.
      If the second root contains signatures present in the first root,
      the corresponding "available since" versions are taken from the first one,
      and the second is ignored.

      merge-since-data <root one> <root two> <result root>, for example

      java -jar diff-builder.jar merge-since-data path/to/since-IU-182.9.zip path/to/since-IU-183.1 path/to/result

      will merge "available since" external annotations roots for IU-182.9 and IU-183.1
      into one root path/to/result, which can be a directory or a zip archive.
    """.trimIndent()

  override fun execute(freeArgs: List<String>) {
    val rootOne = Paths.get(freeArgs[0])
    val rootTwo = Paths.get(freeArgs[1])
    val resultRoot = Paths.get(freeArgs[2])
    mergeSinceData(rootOne, rootTwo, resultRoot)
  }

  fun mergeSinceData(rootOne: Path, rootTwo: Path, resultRoot: Path) {
    LOG.info("Merging 'API is available since' data from ${rootOne.simpleName} and ${rootTwo.simpleName} and saving result to ${resultRoot.simpleName}")

    //Read the first signatures entirely, to check duplications.
    val firstSinceData = SinceApiReader(rootOne).use {
      it.readSinceApiData()
    }

    val tempRoot = if (resultRoot == rootOne || resultRoot == rootTwo) {
      resultRoot.resolveSibling("temp-" + resultRoot.simpleName)
    } else {
      resultRoot
    }

    try {
      SinceApiWriter(tempRoot).use { mergeWriter ->
        mergeWriter.appendSinceApiData(firstSinceData)

        SinceApiReader(rootTwo).use { readerTwo ->
          readerTwo.readAllSignatures()
              .filterNot { (apiSignature, _) -> apiSignature in firstSinceData }
              .forEach { mergeWriter.appendSignature(it.first, it.second) }
        }
      }
    } finally {
      if (resultRoot != tempRoot) {
        Files.move(tempRoot, resultRoot, StandardCopyOption.REPLACE_EXISTING)
      }
    }
    LOG.info("Since API data from ${rootOne.simpleName} and ${rootTwo.simpleName} have been merged to ${resultRoot.simpleName}")
  }
}

