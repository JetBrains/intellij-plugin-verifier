package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.deleteLogged
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.PluginVerifier
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.VerifierExecutor
import com.jetbrains.pluginverifier.dependencies.resolution.EmptyDependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsProviderImpl
import com.jetbrains.pluginverifier.plugin.PluginFilesBank
import com.jetbrains.pluginverifier.reporting.verification.Reportage
import com.jetbrains.pluginverifier.reporting.verification.Reporters
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.repositories.local.LocalPluginInfo
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import com.jetbrains.pluginverifier.resolution.DefaultClassResolverProvider
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.tests.mocks.TestJdkDescriptorProvider
import com.jetbrains.pluginverifier.verifiers.filter.BundledIdeClassesFilter
import com.jetbrains.pluginverifier.verifiers.filter.DynamicallyLoadedFilter
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

class VerificationRunner {

  fun runPluginVerification(ideaFile: Path, pluginFile: Path): VerificationResult {
    val tempDownloadDir = createTempDir().apply { deleteOnExit() }.toPath()
    val pluginFilesBank = PluginFilesBank.create(MarketplaceRepository(URL("https://unused.com")), tempDownloadDir, DiskSpaceSetting(SpaceAmount.ZERO_SPACE))

    val idePlugin = (IdePluginManager.createManager().createPlugin(pluginFile.toFile()) as PluginCreationSuccess).plugin
    val pluginInfo = LocalPluginInfo(idePlugin)
    val jdkPath = TestJdkDescriptorProvider.getJdkPathForTests()
    val tempFolder = Files.createTempDirectory("")
    try {
      val pluginDetailsProvider = PluginDetailsProviderImpl(tempFolder)
      val pluginDetailsCache = PluginDetailsCache(10, pluginFilesBank, pluginDetailsProvider)
      return IdeDescriptor.create(ideaFile, IdeVersion.createIdeVersion("IU-145.500"), null).use { ideDescriptor ->
        val externalClassesPackageFilter = OptionsParser.getExternalClassesPackageFilter(CmdOpts())
        val reportage = object : Reportage {
          override fun createPluginReporters(pluginInfo: PluginInfo, verificationTarget: VerificationTarget) =
              Reporters()

          override fun logVerificationStage(stageMessage: String) = Unit

          override fun logPluginVerificationIgnored(pluginInfo: PluginInfo, verificationTarget: VerificationTarget, reason: String) = Unit

          override fun close() = Unit
        }

        JdkDescriptorsCache().use { jdkDescriptorCache ->
          val tasks = listOf(
              PluginVerifier(
                  pluginInfo,
                  reportage,
                  emptyList(),
                  true,
                  pluginDetailsCache,
                  DefaultClassResolverProvider(
                      EmptyDependencyFinder,
                      jdkDescriptorCache,
                      jdkPath,
                      ideDescriptor,
                      externalClassesPackageFilter
                  ),
                  VerificationTarget.Ide(ideDescriptor.ideVersion),
                  ideDescriptor.brokenPlugins,
                  listOf(DynamicallyLoadedFilter(), BundledIdeClassesFilter)
              )
          )

          VerifierExecutor(4, reportage).use { verifierExecutor ->
            verifierExecutor.verify(tasks).single()
          }
        }
      }
    } finally {
      tempFolder.deleteLogged()
    }
  }

}