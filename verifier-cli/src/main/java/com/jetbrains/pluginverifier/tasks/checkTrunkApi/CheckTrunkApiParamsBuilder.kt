package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.ide.IdeResourceUtil
import com.jetbrains.pluginverifier.misc.closeOnException
import com.jetbrains.pluginverifier.misc.isDirectory
import com.jetbrains.pluginverifier.misc.listPresentationInColumns
import com.jetbrains.pluginverifier.misc.tryInvokeSeveralTimes
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.options.PluginsSet
import com.jetbrains.pluginverifier.options.filter.ExcludedPluginFilter
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.repository.files.FileLock
import com.jetbrains.pluginverifier.repository.files.IdleFileLock
import com.jetbrains.pluginverifier.repository.local.LocalPluginRepositoryFactory
import com.jetbrains.pluginverifier.tasks.TaskParametersBuilder
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class CheckTrunkApiParamsBuilder(private val pluginRepository: PluginRepository,
                                 private val ideFilesBank: IdeFilesBank,
                                 private val verificationReportage: VerificationReportage) : TaskParametersBuilder {

  override fun build(opts: CmdOpts, freeArgs: List<String>): CheckTrunkApiParams {
    val apiOpts = CheckTrunkApiOpts()
    val args = Args.parse(apiOpts, freeArgs.toTypedArray(), false)
    if (args.isEmpty()) {
      throw IllegalArgumentException("The IDE to be checked is not specified")
    }

    verificationReportage.logVerificationStage("Reading classes of the trunk IDE ${args[0]}")
    val trunkIdeDescriptor = OptionsParser.createIdeDescriptor(Paths.get(args[0]), opts)
    return trunkIdeDescriptor.closeOnException {
      buildParameters(opts, apiOpts, trunkIdeDescriptor)
    }
  }

  private fun buildParameters(opts: CmdOpts, apiOpts: CheckTrunkApiOpts, trunkIdeDescriptor: IdeDescriptor): CheckTrunkApiParams {
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
        releaseIdeFileLock = tryInvokeSeveralTimes(3, 5, TimeUnit.SECONDS, "download ide $ideVersion") {
          val result = ideFilesBank.getIdeFile(ideVersion)
          if (result is IdeFilesBank.Result.Found) {
            result.ideFileLock
          } else {
            throw RuntimeException("IDE $ideVersion is not found in $ideFilesBank")
          }
        }
        deleteReleaseIdeOnExit = !apiOpts.saveMajorIdeFile
      }
      else -> throw IllegalArgumentException("Neither the version (-miv) nor the path to the IDE (-mip) with which to compare API problems are specified")
    }

    verificationReportage.logVerificationStage("Reading classes of the release IDE ${releaseIdeFileLock.file}")
    val releaseIdeDescriptor = OptionsParser.createIdeDescriptor(releaseIdeFileLock.file, opts)
    return releaseIdeDescriptor.closeOnException {
      releaseIdeFileLock.closeOnException {
        buildParameters(opts, apiOpts, releaseIdeDescriptor, trunkIdeDescriptor, deleteReleaseIdeOnExit, releaseIdeFileLock)
      }
    }
  }

  private fun buildParameters(
      opts: CmdOpts,
      apiOpts: CheckTrunkApiOpts,
      releaseIdeDescriptor: IdeDescriptor,
      trunkIdeDescriptor: IdeDescriptor,
      deleteReleaseIdeOnExit: Boolean,
      releaseIdeFileLock: FileLock
  ): CheckTrunkApiParams {
    val externalClassesPrefixes = OptionsParser.getExternalClassesPrefixes(opts)
    val problemsFilters = OptionsParser.getProblemsFilters(opts)

    val releaseVersion = releaseIdeDescriptor.ideVersion
    val trunkVersion = trunkIdeDescriptor.ideVersion

    val releaseLocalRepository = apiOpts.releaseLocalPluginRepositoryRoot
        ?.let { LocalPluginRepositoryFactory.createLocalPluginRepository(Paths.get(it)) }

    val trunkLocalRepository = apiOpts.trunkLocalPluginRepositoryRoot
        ?.let { LocalPluginRepositoryFactory.createLocalPluginRepository(Paths.get(it)) }

    val jetBrainsPluginIds = getJetBrainsPluginIds(apiOpts)

    verificationReportage.logVerificationStage("Requesting a list of plugins compatible with the RELEASE IDE $releaseVersion")
    val releaseCompatibleVersions = pluginRepository.tryInvokeSeveralTimes(3, 5, TimeUnit.SECONDS, "fetch last compatible updates with $releaseVersion") {
      getLastCompatiblePlugins(releaseVersion)
    }

    val pluginsSet = PluginsSet()
    pluginsSet.schedulePlugins(
        releaseCompatibleVersions
            .filterNot { it.pluginId in jetBrainsPluginIds }
            .sortedByDescending { (it as UpdateInfo).updateId }
    )

    val allBrokenPlugins = (IdeResourceUtil.getBrokenPluginsListedInIde(releaseIdeDescriptor.ide) +
        IdeResourceUtil.getBrokenPluginsListedInIde(trunkIdeDescriptor.ide))

    pluginsSet.addPluginFilter(ExcludedPluginFilter(allBrokenPlugins))

    println("The following updates will be checked with both #$trunkVersion and #$releaseVersion:\n" +
        pluginsSet.pluginsToCheck
            .sortedBy { (it as UpdateInfo).updateId }
            .listPresentationInColumns(4, 60)
    )

    pluginsSet.ignoredPlugins.forEach { plugin, reason ->
      verificationReportage.logPluginVerificationIgnored(plugin, VerificationTarget.Ide(releaseVersion), reason)
      verificationReportage.logPluginVerificationIgnored(plugin, VerificationTarget.Ide(trunkVersion), reason)
    }

    return CheckTrunkApiParams(
        pluginsSet,
        OptionsParser.getJdkPath(opts),
        trunkIdeDescriptor,
        releaseIdeDescriptor,
        externalClassesPrefixes,
        problemsFilters,
        jetBrainsPluginIds,
        deleteReleaseIdeOnExit,
        releaseIdeFileLock,
        releaseLocalRepository,
        trunkLocalRepository
    )
  }

  private fun getJetBrainsPluginIds(apiOpts: CheckTrunkApiOpts): List<String> {
    if (apiOpts.jetBrainsPluginsFile != null) {
      val file = File(apiOpts.jetBrainsPluginsFile)
      require(file.exists()) { "JetBrains plugin IDS file doesn't exist: $file" }
      return file.readLines()
    }
    return emptyList()
  }

  private fun parseIdeVersion(ideVersion: String) = IdeVersion.createIdeVersionIfValid(ideVersion)
      ?: throw IllegalArgumentException("Invalid IDE version: $ideVersion. Please provide IDE version (with product ID) with which to compare API problems; " +
          "See https://www.jetbrains.com/intellij-repository/releases/")

}

class CheckTrunkApiOpts {
  @set:Argument("major-ide-version", alias = "miv", description = "The IDE version with which to compare API problems. This IDE will be downloaded from the IDE builds repository: https://www.jetbrains.com/intellij-repository/releases/.")
  var majorIdeVersion: String? = null

  @set:Argument("save-major-ide-file", alias = "smif", description = "Whether to save a downloaded release IDE in cache directory for use in later verifications")
  var saveMajorIdeFile: Boolean = false

  @set:Argument("major-ide-path", alias = "mip", description = "The path to release (major) IDE build with which to compare API problems in trunk (master) IDE build.")
  var majorIdePath: String? = null

  @set:Argument("jetbrains-plugins-file", alias = "jbpf", description = "The path to a file with plugin ids separated by newline. " +
      "The provided plugin ids are JetBrains-developed plugins that in conjunction with IDE build constitute IntelliJ API used by third-party plugin developers. " +
      "Compatible versions of these plugins will be downloaded and installed to the release and trunk IDE before verification. " +
      "Found compatibility problems differences will be reported as if it were breakages of trunk API compared to release API.")
  var jetBrainsPluginsFile: String? = null

  @set:Argument("release-jetbrains-plugins", alias = "rjbp", description = "The root of the local plugin repository containing JetBrains plugins compatible with the release IDE. " +
      "The local repository is a set of non-bundled JetBrains plugins built from the same sources (see Installers/<artifacts>/IU-plugins). " +
      "The Plugin Verifier will read the plugin descriptors from every plugin-like file under the specified directory." +
      "On the release IDE verification, the JetBrains plugins will be taken from the local repository if present, and from the public repository, otherwise.")
  var releaseLocalPluginRepositoryRoot: String? = null

  @set:Argument("trunk-jetbrains-plugins", alias = "tjbp", description = "The same as --release-local-repository but specifies the local repository of the trunk IDE.")
  var trunkLocalPluginRepositoryRoot: String? = null

}