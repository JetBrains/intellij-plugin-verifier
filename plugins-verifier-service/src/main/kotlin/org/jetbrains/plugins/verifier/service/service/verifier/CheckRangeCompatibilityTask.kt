package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.core.Verification
import com.jetbrains.pluginverifier.core.VerifierTask
import com.jetbrains.pluginverifier.dependencies.resolution.IdeDependencyFinder
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.parameters.ide.IdeCreator
import com.jetbrains.pluginverifier.parameters.ide.IdeDescriptor
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptor
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.reporting.common.LogReporter
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportageImpl
import com.jetbrains.pluginverifier.reporting.verification.VerificationReporterSet
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportersProvider
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.results.Result
import org.jetbrains.plugins.verifier.service.ide.IdeFileLock
import org.jetbrains.plugins.verifier.service.ide.IdeFilesManager
import org.jetbrains.plugins.verifier.service.storage.JdkManager
import org.jetbrains.plugins.verifier.service.tasks.Task
import org.jetbrains.plugins.verifier.service.tasks.TaskProgress
import org.slf4j.LoggerFactory

class CheckRangeCompatibilityTask(private val updateInfo: UpdateInfo,
                                  val pluginCoordinate: PluginCoordinate,
                                  private val params: CheckRangeParams,
                                  private val ideVersions: List<IdeVersion>? = null,
                                  private val pluginRepository: PluginRepository,
                                  private val pluginDetailsProvider: PluginDetailsProvider) : Task<CheckRangeCompatibilityResult>() {
  companion object {
    private val LOG = LoggerFactory.getLogger(CheckRangeCompatibilityTask::class.java)
  }

  override fun presentableName(): String = "Check $pluginCoordinate with IDE from [since; until]"

  private fun doRangeVerification(plugin: IdePlugin, progress: TaskProgress): CheckRangeCompatibilityResult {
    val sinceBuild = plugin.sinceBuild!!
    val untilBuild = plugin.untilBuild
    val jdkDescriptor = JdkDescriptor(JdkManager.getJdkHome(params.jdkVersion))

    val ideLocks = getAvailableIdesMatchingSinceUntilBuild(sinceBuild, untilBuild)
    try {
      LOG.info("Verifying plugin $plugin [$sinceBuild; $untilBuild]; with available IDEs: ${ideLocks.joinToString()}")
      if (ideLocks.isEmpty()) {
        return CheckRangeCompatibilityResult(updateInfo, CheckRangeCompatibilityResult.ResultType.NO_COMPATIBLE_IDES)
      }
      return checkPluginWithIdes(pluginCoordinate, updateInfo, ideLocks, jdkDescriptor, progress)
    } finally {
      ideLocks.forEach { it.close() }
    }
  }

  private fun checkPluginWithIdes(pluginCoordinate: PluginCoordinate,
                                  updateInfo: UpdateInfo,
                                  ideLocks: List<IdeFileLock>,
                                  jdkDescriptor: JdkDescriptor,
                                  progress: TaskProgress): CheckRangeCompatibilityResult {
    val ideDescriptors = arrayListOf<IdeDescriptor>()
    try {
      ideLocks.mapTo(ideDescriptors) { IdeCreator.createByFile(it.ideFile, null) }
      return checkPluginWithSeveralIdes(pluginCoordinate, updateInfo, ideDescriptors, jdkDescriptor, progress)
    } finally {
      ideDescriptors.forEach { it.close() }
    }
  }

  private class DelegateProgressReporter(private val taskProgress: TaskProgress) : Reporter<Double> {
    override fun report(t: Double) {
      taskProgress.setFraction(t)
    }

    override fun close() = Unit
  }

  private fun checkPluginWithSeveralIdes(pluginCoordinate: PluginCoordinate,
                                         updateInfo: UpdateInfo,
                                         ideDescriptors: List<IdeDescriptor>,
                                         jdkDescriptor: JdkDescriptor,
                                         progress: TaskProgress): CheckRangeCompatibilityResult {
    val verificationReportage = createVerificationReportage(progress)
    val allResults = arrayListOf<Result>()
    for (ideDescriptor in ideDescriptors) {
      val dependencyFinder = IdeDependencyFinder(ideDescriptor.ide, pluginRepository, pluginDetailsProvider)
      val verifierParameters = VerifierParameters(
          externalClassesPrefixes = emptyList(),
          problemFilters = emptyList(),
          externalClassPath = EmptyResolver,
          findDeprecatedApiUsages = true
      )
      val results = Verification.run(verifierParameters, pluginDetailsProvider, listOf(VerifierTask(pluginCoordinate, ideDescriptor, dependencyFinder)), verificationReportage, jdkDescriptor)
      allResults.addAll(results)
    }
    return CheckRangeCompatibilityResult(updateInfo, CheckRangeCompatibilityResult.ResultType.VERIFICATION_DONE, allResults)
  }

  private fun createVerificationReportage(progress: TaskProgress) = VerificationReportageImpl(
      reporterSetProvider = object : VerificationReportersProvider {

        override val globalMessageReporters: List<Reporter<String>> = listOf(LogReporter(LOG))

        override val globalProgressReporters: List<Reporter<Double>> = listOf(DelegateProgressReporter(progress))

        override fun close() = Unit

        override fun getReporterSetForPluginVerification(pluginCoordinate: PluginCoordinate, ideVersion: IdeVersion): VerificationReporterSet {
          return VerificationReporterSet(
              verdictReporters = listOf(LogReporter(LOG)),
              messageReporters = listOf(LogReporter(LOG)),
              progressReporters = listOf(DelegateProgressReporter(progress)),
              warningReporters = emptyList(),
              problemsReporters = emptyList(),
              dependenciesGraphReporters = listOf(LogReporter(LOG)),
              ignoredProblemReporters = emptyList(),
              deprecatedReporters = emptyList()
          )
        }
      }
  )

  private fun getAvailableIdesMatchingSinceUntilBuild(sinceBuild: IdeVersion, untilBuild: IdeVersion?): List<IdeFileLock> = IdeFilesManager.lockAndAccess {
    (ideVersions ?: IdeFilesManager.ideList())
        .filter { sinceBuild <= it && (untilBuild == null || it <= untilBuild) }
        .mapNotNull { IdeFilesManager.getIdeLock(it) }
  }

  override fun computeResult(progress: TaskProgress): CheckRangeCompatibilityResult =
      pluginDetailsProvider.providePluginDetails(pluginCoordinate).use { pluginDetails ->
        when (pluginDetails) {
          is PluginDetails.NotFound -> CheckRangeCompatibilityResult(updateInfo, CheckRangeCompatibilityResult.ResultType.NON_DOWNLOADABLE, nonDownloadableReason = pluginDetails.reason)
          is PluginDetails.FailedToDownload -> CheckRangeCompatibilityResult(updateInfo, CheckRangeCompatibilityResult.ResultType.NON_DOWNLOADABLE, nonDownloadableReason = pluginDetails.reason)
          is PluginDetails.BadPlugin -> CheckRangeCompatibilityResult(updateInfo, CheckRangeCompatibilityResult.ResultType.INVALID_PLUGIN, invalidPluginProblems = pluginDetails.pluginErrorsAndWarnings)
          is PluginDetails.ByFileLock -> doRangeVerification(pluginDetails.plugin, progress)
          is PluginDetails.FoundOpenPluginAndClasses -> doRangeVerification(pluginDetails.plugin, progress)
          is PluginDetails.FoundOpenPluginWithoutClasses -> doRangeVerification(pluginDetails.plugin, progress)
        }
      }
}