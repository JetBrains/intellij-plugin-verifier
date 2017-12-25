package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.core.Verification
import com.jetbrains.pluginverifier.core.VerifierTask
import com.jetbrains.pluginverifier.dependencies.resolution.IdeDependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptor
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.reporting.common.LogReporter
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportageImpl
import com.jetbrains.pluginverifier.reporting.verification.VerificationReporterSet
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportersProvider
import com.jetbrains.pluginverifier.repository.UpdateInfo
import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.service.jdks.JdkVersion
import org.jetbrains.plugins.verifier.service.service.verifier.CheckRangeTask.Result
import org.jetbrains.plugins.verifier.service.tasks.ProgressIndicator
import org.jetbrains.plugins.verifier.service.tasks.ServiceTask
import org.slf4j.LoggerFactory

/**
 * The service task that runs the plugin verification
 * of the [updateInfo] against (since; until)-compatible [toCheckIdeVersions] and
 * returns the aggregated [result] [Result].
 */
class CheckRangeTask(val updateInfo: UpdateInfo,
                     private val jdkVersion: JdkVersion,
                     private val toCheckIdeVersions: List<IdeVersion>,
                     val serverContext: ServerContext) : ServiceTask<CheckRangeTask.Result>("Check $updateInfo with IDE from [since; until]") {

  companion object {
    private val LOG = LoggerFactory.getLogger(CheckRangeTask::class.java)
  }

  data class Result(val updateInfo: UpdateInfo,
                    val resultType: ResultType,
                    val verificationResults: List<com.jetbrains.pluginverifier.results.Result>? = null,
                    val invalidPluginProblems: List<PluginProblem>? = null,
                    val nonDownloadableReason: String? = null) {
    enum class ResultType {
      NON_DOWNLOADABLE,
      NO_COMPATIBLE_IDES,
      INVALID_PLUGIN,
      VERIFICATION_DONE
    }

  }

  override fun execute(progress: ProgressIndicator): Result =
      serverContext.pluginDetailsCache.getPluginDetails(updateInfo).use { pluginDetailsCacheEntry ->
        val pluginDetails = pluginDetailsCacheEntry.resource
        with(pluginDetails) {
          when (this) {
            is PluginDetails.NotFound -> Result(
                updateInfo,
                Result.ResultType.NON_DOWNLOADABLE,
                nonDownloadableReason = reason
            )
            is PluginDetails.FailedToDownload -> Result(
                updateInfo,
                Result.ResultType.NON_DOWNLOADABLE,
                nonDownloadableReason = reason
            )
            is PluginDetails.BadPlugin -> Result(
                updateInfo,
                Result.ResultType.INVALID_PLUGIN,
                invalidPluginProblems = pluginErrorsAndWarnings
            )
            is PluginDetails.ByFileLock,
            is PluginDetails.FoundOpenPluginAndClasses,
            is PluginDetails.FoundOpenPluginWithoutClasses -> doVerification(progress)
          }
        }
      }

  private fun doVerification(progress: ProgressIndicator): Result {
    val ideDescriptorEntries = serverContext.ideDescriptorsCache.getIdeDescriptors { availableIdeVersions ->
      availableIdeVersions.filter {
        it in toCheckIdeVersions && updateInfo.isCompatibleWith(it)
      }
    }

    return try {
      if (ideDescriptorEntries.isEmpty()) {
        Result(updateInfo, Result.ResultType.NO_COMPATIBLE_IDES)
      } else {
        val ideDescriptors = ideDescriptorEntries.map { it.resource }
        checkPluginWithIdes(ideDescriptors, progress)
      }
    } finally {
      ideDescriptorEntries.forEach { it.close() }
    }
  }

  private fun checkPluginWithIdes(ideDescriptors: List<IdeDescriptor>, progress: ProgressIndicator): Result {
    val verificationReportage = createVerificationReportage(progress)
    val allResults = ideDescriptors.flatMap { checkPluginWithIde(it, verificationReportage) }
    return Result(updateInfo, Result.ResultType.VERIFICATION_DONE, allResults)
  }

  private fun checkPluginWithIde(ideDescriptor: IdeDescriptor, verificationReportage: VerificationReportageImpl): List<com.jetbrains.pluginverifier.results.Result> {
    val dependencyFinder = IdeDependencyFinder(
        ideDescriptor.ide,
        serverContext.pluginRepository,
        serverContext.pluginDetailsProvider
    )

    val verifierParameters = VerifierParameters(
        externalClassesPrefixes = emptyList(),
        problemFilters = emptyList(),
        externalClassPath = EmptyResolver,
        findDeprecatedApiUsages = true
    )

    val jdkDescriptor = JdkDescriptor(serverContext.jdkManager.getJdkHome(jdkVersion))
    val pluginCoordinate = PluginCoordinate.ByUpdateInfo(updateInfo, serverContext.pluginRepository)
    val verifierTask = VerifierTask(pluginCoordinate, ideDescriptor, dependencyFinder)

    return Verification.run(
        verifierParameters,
        serverContext.pluginDetailsProvider,
        listOf(verifierTask),
        verificationReportage,
        jdkDescriptor
    )
  }

  private fun createVerificationReportage(progress: ProgressIndicator) = VerificationReportageImpl(
      reporterSetProvider = object : VerificationReportersProvider {

        override val globalMessageReporters = listOf<Reporter<String>>(LogReporter(LOG))

        override val globalProgressReporters = listOf(DelegateProgressReporter(progress))

        override fun close() = Unit

        override fun getReporterSetForPluginVerification(pluginCoordinate: PluginCoordinate, ideVersion: IdeVersion) =
            VerificationReporterSet(
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
  )

}