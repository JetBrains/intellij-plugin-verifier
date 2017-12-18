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
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.reporting.common.LogReporter
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportageImpl
import com.jetbrains.pluginverifier.reporting.verification.VerificationReporterSet
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportersProvider
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.repository.files.FileLock
import com.jetbrains.pluginverifier.results.Result
import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.service.jdks.JdkVersion
import org.jetbrains.plugins.verifier.service.tasks.ProgressIndicator
import org.jetbrains.plugins.verifier.service.tasks.ServiceTask
import org.slf4j.LoggerFactory

/**
 * The service task which runs the plugin verification
 * of the [updateInfo] against several IDE builds and
 * composes the [result] [CheckRangeCompatibilityResult].
 *
 * The IDE builds are chosen from the set of [available] [org.jetbrains.plugins.verifier.service.service.ide.IdeKeeper] IDEs
 * so that they match the [since; until] compatibility range of the plugin.
 */
class CheckRangeCompatibilityServiceTask(private val updateInfo: UpdateInfo,
                                         private val jdkVersion: JdkVersion,
                                         private val ideVersions: List<IdeVersion>,
                                         val serverContext: ServerContext) : ServiceTask<CheckRangeCompatibilityResult>("Check $updateInfo with IDE from [since; until]") {
  companion object {
    private val LOG = LoggerFactory.getLogger(CheckRangeCompatibilityServiceTask::class.java)
  }

  val pluginCoordinate = PluginCoordinate.ByUpdateInfo(updateInfo, serverContext.pluginRepository)

  private fun doRangeVerification(plugin: IdePlugin, progress: ProgressIndicator): CheckRangeCompatibilityResult {
    val sinceBuild = plugin.sinceBuild!!
    val untilBuild = plugin.untilBuild
    val jdkDescriptor = JdkDescriptor(serverContext.jdkManager.getJdkHome(jdkVersion))

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
                                  ideLocks: List<FileLock>,
                                  jdkDescriptor: JdkDescriptor,
                                  progress: ProgressIndicator): CheckRangeCompatibilityResult {
    val ideDescriptors = arrayListOf<IdeDescriptor>()
    try {
      ideLocks.mapTo(ideDescriptors) { IdeCreator.createByFile(it.file, null) }
      return checkPluginWithSeveralIdes(pluginCoordinate, updateInfo, ideDescriptors, jdkDescriptor, progress)
    } finally {
      ideDescriptors.forEach { it.close() }
    }
  }

  private class DelegateProgressReporter(private val taskProgress: ProgressIndicator) : Reporter<Double> {
    override fun report(t: Double) {
      taskProgress.fraction = t
    }

    override fun close() = Unit
  }

  private fun checkPluginWithSeveralIdes(pluginCoordinate: PluginCoordinate,
                                         updateInfo: UpdateInfo,
                                         ideDescriptors: List<IdeDescriptor>,
                                         jdkDescriptor: JdkDescriptor,
                                         progress: ProgressIndicator): CheckRangeCompatibilityResult {
    val verificationReportage = createVerificationReportage(progress)
    val allResults = arrayListOf<Result>()
    for (ideDescriptor in ideDescriptors) {
      val dependencyFinder = IdeDependencyFinder(ideDescriptor.ide, serverContext.pluginRepository, serverContext.pluginDetailsProvider)
      val verifierParameters = VerifierParameters(
          externalClassesPrefixes = emptyList(),
          problemFilters = emptyList(),
          externalClassPath = EmptyResolver,
          findDeprecatedApiUsages = true
      )
      val results = Verification.run(verifierParameters, serverContext.pluginDetailsProvider, listOf(VerifierTask(pluginCoordinate, ideDescriptor, dependencyFinder)), verificationReportage, jdkDescriptor)
      allResults.addAll(results)
    }
    return CheckRangeCompatibilityResult(updateInfo, CheckRangeCompatibilityResult.ResultType.VERIFICATION_DONE, allResults)
  }

  private fun createVerificationReportage(progress: ProgressIndicator) = VerificationReportageImpl(
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

  private fun getAvailableIdesMatchingSinceUntilBuild(sinceBuild: IdeVersion, untilBuild: IdeVersion?): List<FileLock> = serverContext.ideFilesBank.lockAndAccess {
    ideVersions
        .filter { sinceBuild <= it && (untilBuild == null || it <= untilBuild) }
        .mapNotNull { (serverContext.ideFilesBank.getIdeLock(it)) }
  }

  override fun execute(progress: ProgressIndicator): CheckRangeCompatibilityResult =
      serverContext.pluginDetailsProvider.providePluginDetails(pluginCoordinate).use { pluginDetails ->
        with(pluginDetails) {
          when (this) {
            is PluginDetails.NotFound -> CheckRangeCompatibilityResult(updateInfo, CheckRangeCompatibilityResult.ResultType.NON_DOWNLOADABLE, nonDownloadableReason = reason)
            is PluginDetails.FailedToDownload -> CheckRangeCompatibilityResult(updateInfo, CheckRangeCompatibilityResult.ResultType.NON_DOWNLOADABLE, nonDownloadableReason = reason)
            is PluginDetails.BadPlugin -> CheckRangeCompatibilityResult(updateInfo, CheckRangeCompatibilityResult.ResultType.INVALID_PLUGIN, invalidPluginProblems = pluginErrorsAndWarnings)
            is PluginDetails.ByFileLock -> doRangeVerification(plugin, progress)
            is PluginDetails.FoundOpenPluginAndClasses -> doRangeVerification(plugin, progress)
            is PluginDetails.FoundOpenPluginWithoutClasses -> doRangeVerification(plugin, progress)
          }
        }
      }
}