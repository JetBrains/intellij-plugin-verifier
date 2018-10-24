package org.jetbrains.ide.diff.builder.cli

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.IdeRepository
import com.jetbrains.pluginverifier.ide.IntelliJIdeRepository
import com.sampullara.cli.Args
import com.sampullara.cli.Argument

/**
 * Utility command that lists all IDE builds
 * available in the IntelliJ artifacts repositories.
 * @see [help]
 */
class IdeRepositoryIndexCommand : Command {
  override val commandName
    get() = "ide-repository-index"

  override val help
    get() = """
      Utility command that lists all IDE builds available in IntelliJ artifact repositories
       https://www.jetbrains.com/intellij-repository/releases/ and
       https://www.jetbrains.com/intellij-repository/snapshots

      ide-repository-index [-channel <option>] [-skip-release-version] [-product-code <code>] [-ascending]
        -channel <option> := all (default) | releases-only | snapshots-only
        -channel option is used to specify a channel of the IDE repository
        to list builds from.

        -skip-release-version - specify this option if you want only build numbers to be printed.
        If not specified, release versions of corresponding IDE builds are printed too, like:
        IU-182.4892.20 (2018.2.5)

        -product-code <code> - specify this option if you want to list only IDEs of corresponding product
        (IU, IC, RD and so one)

        -ascending (default: false)  - specify this option if you want to print IDE versions
        in ascending order

      For example,
      java -jar ide-diff-builder.jar ide-repository-index -channel all

      lists all IDE builds available in /releases and /snapshots channels
      of the IntelliJ artifacts repository.
    """.trimIndent()

  open class Options {
    @set:Argument("channel", description = "Channel of IDE repository: all | releases-only | snapshots-only")
    var channel: String? = null

    @set:Argument("product-code", description = "Product code of IDEs to list")
    var productCode: String? = null

    @set:Argument("skip-release-version", description = "If specified, the release versions will not be printed, but only build numbers")
    var skipReleaseVersion: Boolean = false

    @set:Argument("ascending", description = "If specified, the versions are printed in ascending order. Otherwise in descending order.")
    var ascendingOrder: Boolean = false
  }

  override fun execute(freeArgs: List<String>) {
    val options = Options()
    Args.parse(options, freeArgs.toTypedArray(), false)
    val repository = when (options.channel) {
      "all" -> allIdeRepository
      "releases-only" -> releasesIdeRepository
      "snapshots-only" -> snapshotsIdeRepository
      else -> allIdeRepository
    }

    val index = repository
        .fetchIndex()
        .filter { options.productCode == null || options.productCode == it.version.productCode }
        .sortedByDescending { it.version }

    val sortedIdes = if (options.ascendingOrder) {
      index.sortedBy { it.version }
    } else {
      index.sortedByDescending { it.version }
    }

    for (availableIde in sortedIdes) {
      if (options.skipReleaseVersion) {
        println(availableIde.version.asString())
      } else {
        println(availableIde)
      }
    }
  }

}

val releasesIdeRepository = IntelliJIdeRepository(false)
val snapshotsIdeRepository = IntelliJIdeRepository(true)

val allIdeRepository = MergingIdeRepository(listOf(
    releasesIdeRepository,
    snapshotsIdeRepository
))

class MergingIdeRepository(private val ideRepositories: List<IdeRepository>) : IdeRepository {

  override fun fetchIndex() =
      ideRepositories.flatMap { it.fetchIndex() }.distinctBy { it.version }.sortedBy { it.version }

  override fun fetchAvailableIde(ideVersion: IdeVersion) =
      ideRepositories.asSequence().mapNotNull { it.fetchAvailableIde(ideVersion) }.firstOrNull()

}
