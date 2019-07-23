package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.PluginVerifier
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.dependencies.resolution.BundledPluginDependencyFinder
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
import com.jetbrains.pluginverifier.verifiers.filter.DynamicallyLoadedFilter
import java.net.URL
import java.nio.file.Files

class VerificationRunner {

  fun runPluginVerification(ide: Ide, idePlugin: IdePlugin): VerificationResult {
    val tempDownloadDir = createTempDir().apply { deleteOnExit() }.toPath()
    val pluginFilesBank = PluginFilesBank.create(MarketplaceRepository(URL("https://unused.com")), tempDownloadDir, DiskSpaceSetting(SpaceAmount.ZERO_SPACE))

    val jdkPath = TestJdkDescriptorProvider.getJdkPathForTests()
    val tempFolder = Files.createTempDirectory("")
    tempFolder.toFile().deleteOnExit()

    val pluginDetailsProvider = PluginDetailsProviderImpl(tempFolder)
    val pluginDetailsCache = PluginDetailsCache(10, pluginFilesBank, pluginDetailsProvider)
    return IdeDescriptor.create(ide.idePath.toPath(), null, null).use { ideDescriptor ->
      val externalClassesPackageFilter = OptionsParser.getExternalClassesPackageFilter(CmdOpts())
      val reportage = object : Reportage {
        override fun createPluginReporters(pluginInfo: PluginInfo, verificationTarget: VerificationTarget) =
            Reporters()

        override fun logVerificationStage(stageMessage: String) = Unit

        override fun logPluginVerificationIgnored(pluginInfo: PluginInfo, verificationTarget: VerificationTarget, reason: String) = Unit

        override fun close() = Unit
      }

      JdkDescriptorsCache().use { jdkDescriptorCache ->
        val pluginVerifier = PluginVerifier(
            LocalPluginInfo(idePlugin),
            reportage,
            emptyList(),
            true,
            pluginDetailsCache,
            DefaultClassResolverProvider(
                BundledPluginDependencyFinder(ide, pluginDetailsCache),
                jdkDescriptorCache,
                jdkPath,
                ideDescriptor,
                externalClassesPackageFilter
            ),
            VerificationTarget.Ide(ideDescriptor.ideVersion),
            ideDescriptor.brokenPlugins,
            listOf(DynamicallyLoadedFilter())
        )
        pluginVerifier.call()
      }
    }
  }

}