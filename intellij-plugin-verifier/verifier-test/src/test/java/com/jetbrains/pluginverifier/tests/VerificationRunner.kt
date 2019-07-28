package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.PluginVerifier
import com.jetbrains.pluginverifier.dependencies.resolution.BundledPluginDependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsProviderImpl
import com.jetbrains.pluginverifier.plugin.PluginFilesBank
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.repositories.local.LocalPluginInfo
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import com.jetbrains.pluginverifier.resolution.DefaultClassResolverProvider
import com.jetbrains.pluginverifier.tests.mocks.TestJdkDescriptorProvider
import com.jetbrains.pluginverifier.verifiers.filter.DynamicallyLoadedFilter
import java.net.URL
import java.nio.file.Files

class VerificationRunner {

  fun runPluginVerification(ide: Ide, idePlugin: IdePlugin): PluginVerificationResult {
    val tempDownloadDir = createTempDir().apply { deleteOnExit() }.toPath()
    val pluginFilesBank = PluginFilesBank.create(MarketplaceRepository(URL("https://unused.com")), tempDownloadDir, DiskSpaceSetting(SpaceAmount.ZERO_SPACE))

    val jdkPath = TestJdkDescriptorProvider.getJdkPathForTests()
    val tempFolder = Files.createTempDirectory("")
    tempFolder.toFile().deleteOnExit()

    val pluginDetailsProvider = PluginDetailsProviderImpl(tempFolder)
    val pluginDetailsCache = PluginDetailsCache(10, pluginFilesBank, pluginDetailsProvider)
    return IdeDescriptor.create(ide.idePath.toPath(), null, null).use { ideDescriptor ->
      val externalClassesPackageFilter = OptionsParser.getExternalClassesPackageFilter(CmdOpts())

      JdkDescriptorsCache().use { jdkDescriptorCache ->
        val pluginVerifier = PluginVerifier(
            LocalPluginInfo(idePlugin),
            PluginVerificationTarget.IDE(ideDescriptor.ide),
            emptyList(),
            pluginDetailsCache,
            DefaultClassResolverProvider(
                BundledPluginDependencyFinder(ide, pluginDetailsCache),
                jdkDescriptorCache,
                jdkPath,
                ideDescriptor,
                externalClassesPackageFilter
            ),
            listOf(DynamicallyLoadedFilter())
        )
        pluginVerifier.loadPluginAndVerify()
      }
    }
  }

}