package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.misc.isDirectory
import com.jetbrains.pluginverifier.misc.listPresentationInColumns
import com.jetbrains.pluginverifier.misc.tryInvokeSeveralTimes
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptor
import com.jetbrains.pluginverifier.plugin.IdleFileLock
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.files.FileLock
import com.jetbrains.pluginverifier.repository.local.LocalPluginRepositoryFactory
import com.jetbrains.pluginverifier.tasks.TaskParametersBuilder
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Patrikeev
 */
class CheckTrunkApiParamsBuilder(val pluginRepository: PluginRepository,
                                 val ideFilesBank: IdeFilesBank) : TaskParametersBuilder {

  //todo: close the IdeDescriptors in case of exception
  override fun build(opts: CmdOpts, freeArgs: List<String>): CheckTrunkApiParams {
    val apiOpts = CheckTrunkApiOpts()
    val args = Args.parse(apiOpts, freeArgs.toTypedArray(), false)
    if (args.isEmpty()) {
      throw IllegalArgumentException("The IDE to be checked is not specified")
    }

    val trunkIdeDescriptor = OptionsParser.createIdeDescriptor(Paths.get(args[0]), opts)
    val jdkDescriptor = JdkDescriptor(OptionsParser.getJdkDir(opts))

    val releaseIdeFileLock: FileLock
    val deleteReleaseIdeOnExit: Boolean

    when {
      apiOpts.majorIdePath != null -> {
        val majorPath = Paths.get(apiOpts.majorIdePath)
        if (!majorPath.isDirectory) {
          throw IllegalArgumentException("The specified major IDE doesn't exist: $majorPath")
        }
        releaseIdeFileLock = IdleFileLock(majorPath)
        deleteReleaseIdeOnExit = false
      }
      apiOpts.majorIdeVersion != null -> {
        val ideVersion = parseIdeVersion(apiOpts.majorIdeVersion!!)
        releaseIdeFileLock = this.tryInvokeSeveralTimes(3, 5, TimeUnit.SECONDS, "download ide $ideVersion") {
          downloadIdeByVersion(ideVersion)
        }
        deleteReleaseIdeOnExit = !apiOpts.saveMajorIdeFile
      }
      else -> throw IllegalArgumentException("Neither the version (-miv) nor the path to the IDE (-mip) with which to compare API problems specified")
    }
    val releaseIdeDescriptor = OptionsParser.createIdeDescriptor(releaseIdeFileLock.file, opts)

    val externalClassesPrefixes = OptionsParser.getExternalClassesPrefixes(opts)
    val problemsFilters = OptionsParser.getProblemsFilters(opts)

    val releaseVersion = releaseIdeDescriptor.ideVersion
    val trunkVersion = trunkIdeDescriptor.ideVersion

    val releaseLocalRepository = apiOpts.releaseLocalPluginRepositoryRoot?.let { LocalPluginRepositoryFactory.createLocalPluginRepository(releaseVersion, Paths.get(it)) }
    val trunkLocalRepository = apiOpts.trunkLocalPluginRepositoryRoot?.let { LocalPluginRepositoryFactory.createLocalPluginRepository(trunkVersion, Paths.get(it)) }

    val jetBrainsPluginIds = getJetBrainsPluginIds(apiOpts)

    val releaseCompatibleUpdates = pluginRepository.tryInvokeSeveralTimes(3, 5, TimeUnit.SECONDS, "fetch last compatible updates with $releaseVersion") {
      getLastCompatibleUpdates(releaseVersion)
    }
    val pluginsToCheck = releaseCompatibleUpdates.filterNot { it.pluginId in jetBrainsPluginIds }.sortedByDescending { it.updateId }
    val pluginCoordinates = pluginsToCheck.map { PluginCoordinate.ByUpdateInfo(it, pluginRepository) }

    println("The following updates will be checked with both #$trunkVersion and #$releaseVersion:\n" + pluginsToCheck.sortedBy { it.updateId }.listPresentationInColumns(4, 60))

    return CheckTrunkApiParams(
        trunkIdeDescriptor,
        releaseIdeDescriptor,
        externalClassesPrefixes,
        problemsFilters,
        jdkDescriptor,
        jetBrainsPluginIds,
        deleteReleaseIdeOnExit,
        releaseIdeFileLock,
        releaseLocalRepository,
        trunkLocalRepository,
        pluginCoordinates
    )
  }

  private fun getJetBrainsPluginIds(apiOpts: CheckTrunkApiOpts): List<String> {
    if (apiOpts.jetBrainsPluginsFile != null) {
      return File(apiOpts.jetBrainsPluginsFile).readLines()
    }
    return emptyList()
  }

  private fun downloadIdeByVersion(ideVersion: IdeVersion): FileLock =
      ideFilesBank.getIdeLock(ideVersion)
          ?: throw RuntimeException("IDE $ideVersion is not found in $ideFilesBank")

  private fun parseIdeVersion(ideVersion: String) = IdeVersion.createIdeVersionIfValid(ideVersion)
      ?: throw IllegalArgumentException("Invalid IDE version: $ideVersion. Please provide IDE version (with product ID) with which to compare API problems; " +
      "See https://www.jetbrains.com/intellij-repository/releases/")

  class CheckTrunkApiOpts {
    @set:Argument("major-ide-version", alias = "miv", description = "The IDE version with which to compare API problems")
    var majorIdeVersion: String? = null

    @set:Argument("save-major-ide-file", alias = "smif", description = "Whether to save a downloaded release IDE in cache directory for use in later verifications")
    var saveMajorIdeFile: Boolean = false

    @set:Argument("major-ide-path", alias = "mip", description = "The path to the IDE with which to compare API problems")
    var majorIdePath: String? = null

    @set:Argument("jetbrains-plugins-file", alias = "jbpf", description = "The path to a file with plugin ids separated by newline. " +
        "The provided plugin ids are JetBrains-developed plugins that in conjunction with IDE build constitute IntelliJ API used by third-party plugin developers. " +
        "Compatible versions of these plugins will be downloaded and installed to the release and trunk IDE before verification. " +
        "Found compatibility problems differences will be reported as if it were breakages of trunk API compared to release API.")
    var jetBrainsPluginsFile: String? = null

    @set:Argument("release-jetbrains-plugins", alias = "rjbp", description = "The root of the local plugin repository containing JetBrains plugins compatible with the release IDE. " +
        "The local repository is a set of non-bundled JetBrains plugins built from the same sources (see Installers/<artifacts>/IU-plugins). " +
        "If a meta-file 'plugins.xml' is available, the repository content will be read from it, otherwise we will read the plugin descriptors from every plugin-like file under the specified directory." +
        "On the release IDE verification, the JetBrains plugins will be taken from the local repository if present and from the public repository, otherwise.")
    var releaseLocalPluginRepositoryRoot: String? = null

    @set:Argument("trunk-jetbrains-plugins", alias = "tjbp", description = "The same as --release-local-repository but specifies the local repository of the trunk IDE.")
    var trunkLocalPluginRepositoryRoot: String? = null


  }

}