package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerifier
import com.jetbrains.pluginverifier.dependencies.resolution.BundledPluginDependencyFinder
import com.jetbrains.pluginverifier.filtering.ApiUsageFilter
import com.jetbrains.pluginverifier.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.plugin.PluginDetailsProviderImpl
import com.jetbrains.pluginverifier.plugin.PluginFilesBank
import com.jetbrains.pluginverifier.plugin.SizeLimitedPluginDetailsCache
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.repositories.local.LocalPluginInfo
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import com.jetbrains.pluginverifier.resolution.DefaultClassResolverProvider
import com.jetbrains.pluginverifier.tests.mocks.TestJdkDescriptorProvider
import com.jetbrains.pluginverifier.verifiers.filter.DynamicallyLoadedFilter
import java.net.URL
import java.nio.file.Files
import kotlin.io.path.createTempDirectory

class VerificationRunner {

  fun runPluginVerification(ide: Ide, idePlugin: IdePlugin, problemsFilters: List<ProblemsFilter> = emptyList(), apiUsageFilters: List<ApiUsageFilter> = emptyList()): PluginVerificationResult {
    val tempDownloadDir = createTempDirectory().toFile().apply { deleteOnExit() }.toPath()
    val pluginFilesBank = PluginFilesBank.create(MarketplaceRepository(URL("https://unused.com")), tempDownloadDir, DiskSpaceSetting(SpaceAmount.ZERO_SPACE))

    val jdkPath = TestJdkDescriptorProvider.getJdkPathForTests()
    val tempFolder = Files.createTempDirectory("")
    tempFolder.toFile().deleteOnExit()

    val pluginDetailsProvider = PluginDetailsProviderImpl(tempFolder)
    val pluginDetailsCache = SizeLimitedPluginDetailsCache(10, pluginFilesBank, pluginDetailsProvider)
    return IdeDescriptor.create(ide.idePath, jdkPath, null).use { ideDescriptor ->
      val externalClassesPackageFilter = OptionsParser.getExternalClassesPackageFilter(CmdOpts())

      val classResolverProvider = DefaultClassResolverProvider(
        BundledPluginDependencyFinder(ide, pluginDetailsCache),
        ideDescriptor,
        externalClassesPackageFilter
      )
      val verificationDescriptor = PluginVerificationDescriptor.IDE(ideDescriptor, classResolverProvider, LocalPluginInfo(idePlugin))

      val pluginVerifier = PluginVerifier(
        verificationDescriptor,
        problemsFilters,
        pluginDetailsCache,
        listOf(DynamicallyLoadedFilter()),
          false,
        apiUsageFilters
      )
      pluginVerifier.loadPluginAndVerify()
    }
  }

}