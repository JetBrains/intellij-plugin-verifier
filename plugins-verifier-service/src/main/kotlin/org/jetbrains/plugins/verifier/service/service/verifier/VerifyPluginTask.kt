package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.core.VerifierExecutor
import com.jetbrains.pluginverifier.core.VerifierTask
import com.jetbrains.pluginverifier.dependencies.resolution.IdeDependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.ide.IdeDescriptorsCache
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportageImpl
import com.jetbrains.pluginverifier.reporting.verification.VerificationReporterSet
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportersProvider
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import org.jetbrains.plugins.verifier.service.tasks.ProgressIndicator
import org.jetbrains.plugins.verifier.service.tasks.ServiceTask

/**
 * [ServiceTask] verifies the [plugin] [updateInfo]
 * against the [IDE] [ideVersion] in the [verifierExecutor]
 * using the [JDK] [jdkPath].
 */
class VerifyPluginTask(private val verifierExecutor: VerifierExecutor,
                       private val updateInfo: UpdateInfo,
                       private val ideVersion: IdeVersion,
                       private val jdkPath: JdkPath,
                       private val pluginDetailsCache: PluginDetailsCache,
                       private val ideDescriptorsCache: IdeDescriptorsCache)
  : ServiceTask<VerificationResult>("Check $updateInfo against IDE $ideVersion") {

  override fun execute(progress: ProgressIndicator): VerificationResult {
    return ideDescriptorsCache.getIdeDescriptorCacheEntry(ideVersion).use { entry ->
      val ideDescriptor = entry.resource
      val verificationReportage = createVerificationReportage(progress)
      checkPluginWithIde(ideDescriptor, verificationReportage)
    }
  }

  private fun checkPluginWithIde(ideDescriptor: IdeDescriptor,
                                 verificationReportage: VerificationReportage): VerificationResult {
    val dependencyFinder = IdeDependencyFinder(
        ideDescriptor.ide,
        updateInfo.pluginRepository,
        pluginDetailsCache
    )

    val verifierParameters = VerifierParameters(
        externalClassesPrefixes = emptyList(),
        problemFilters = emptyList(),
        externalClassPath = EmptyResolver,
        findDeprecatedApiUsages = true
    )

    val tasks = listOf(VerifierTask(updateInfo, jdkPath, ideDescriptor, dependencyFinder))
    return verifierExecutor
        .verify(tasks, verifierParameters, verificationReportage)
        .single()
  }

  private fun createDelegatingReporter(progress: ProgressIndicator): Reporter<Double> {
    return object : Reporter<Double> {
      override fun report(t: Double) {
        progress.fraction = t
      }

      override fun close() = Unit
    }
  }

  private fun createVerificationReportage(progress: ProgressIndicator) = VerificationReportageImpl(
      reporterSetProvider = object : VerificationReportersProvider {

        override val globalMessageReporters = listOf<Reporter<String>>()

        override val globalProgressReporters = listOf(createDelegatingReporter(progress))

        override fun close() = Unit

        override fun getReporterSetForPluginVerification(pluginInfo: PluginInfo, ideVersion: IdeVersion) =
            VerificationReporterSet(
                verificationResultReporters = listOf(),
                messageReporters = listOf(),
                progressReporters = listOf(),
                pluginStructureWarningsReporters = emptyList(),
                pluginStructureErrorsReporters = emptyList(),
                problemsReporters = emptyList(),
                dependenciesGraphReporters = listOf(),
                ignoredProblemReporters = emptyList(),
                deprecatedReporters = emptyList(),
                exceptionReporters = listOf()
            )
      }
  )

}