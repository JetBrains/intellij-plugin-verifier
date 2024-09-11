/*
 * Copyright 2000-2023 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.classes.resolvers.JarFileResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver.ReadMode
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.classes.IdeFileOrigin.IdeLibDirectory
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerifier
import com.jetbrains.pluginverifier.dependencies.resolution.BundledPluginDependencyFinder
import com.jetbrains.pluginverifier.filtering.ApiUsageFilter
import com.jetbrains.pluginverifier.filtering.KtInternalModifierUsageFilter
import com.jetbrains.pluginverifier.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.plugin.DefaultPluginDetailsProvider
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

  fun withPluginVerifier(ide: Ide, idePlugin: IdePlugin, problemsFilters: List<ProblemsFilter> = emptyList(), apiUsageFilters: List<ApiUsageFilter> = emptyList(),
                         includeKotlinStdLib: Boolean = false,
                         pluginVerifierHandler: (PluginVerifier) -> PluginVerificationResult): PluginVerificationResult {
    val tempDownloadDir = createTempDirectory().toFile().apply { deleteOnExit() }.toPath()
    val pluginFilesBank = PluginFilesBank.create(MarketplaceRepository(URL("https://unused.com")), tempDownloadDir, DiskSpaceSetting(SpaceAmount.ZERO_SPACE))

    val jdkPath = TestJdkDescriptorProvider.getJdkPathForTests()
    val tempFolder = Files.createTempDirectory("")
    tempFolder.toFile().deleteOnExit()

    val pluginDetailsProvider = DefaultPluginDetailsProvider(tempFolder)
    val pluginDetailsCache = SizeLimitedPluginDetailsCache(10, pluginFilesBank, pluginDetailsProvider)
    return IdeDescriptor.create(ide.idePath, jdkPath, null).use { ideDescriptor ->
      val externalClassesPackageFilter = OptionsParser.getExternalClassesPackageFilter(CmdOpts())

      val additionalResolvers = getAdditionalClassResolvers(ide, includeKotlinStdLib)

      val classResolverProvider = DefaultClassResolverProvider(
        BundledPluginDependencyFinder(ide, pluginDetailsCache),
        ideDescriptor,
        externalClassesPackageFilter,
        additionalResolvers
      )
      val verificationDescriptor = PluginVerificationDescriptor.IDE(ideDescriptor, classResolverProvider, LocalPluginInfo(idePlugin))

      val allApiUsagesFilters = apiUsageFilters + getAdditionalApiUsageFilters(includeKotlinStdLib)

      val pluginVerifier = PluginVerifier(
        verificationDescriptor,
        problemsFilters,
        pluginDetailsCache,
        listOf(DynamicallyLoadedFilter()),
        false,
        allApiUsagesFilters
      )
      pluginVerifierHandler(pluginVerifier)
    }
  }

  fun runPluginVerification(ide: Ide, idePlugin: IdePlugin, problemsFilters: List<ProblemsFilter> = emptyList(), apiUsageFilters: List<ApiUsageFilter> = emptyList()): PluginVerificationResult {
    return withPluginVerifier(ide, idePlugin, problemsFilters, apiUsageFilters) {
      it.loadPluginAndVerify()
    }
  }

  private fun getAdditionalClassResolvers(ide: Ide, includeKotlinStdLib: Boolean): List<JarFileResolver> {
    return if (includeKotlinStdLib) {
      val kotlinStdLibJar = ide.findKotlinStdLib()
      listOf(JarFileResolver(kotlinStdLibJar, ReadMode.SIGNATURES, IdeLibDirectory(ide)))
    } else {
      emptyList()
    }
  }

  private fun getAdditionalApiUsageFilters(includeKotlinStdLib: Boolean): List<ApiUsageFilter> {
    return if(includeKotlinStdLib) {
      listOf(KtInternalModifierUsageFilter())
    } else {
      emptyList()
    }
  }
}

fun runPluginVerification(initSpec: VerificationSpec.() -> Unit): PluginVerificationResult = with(VerificationSpec()) {
  initSpec()
  VerificationRunner().withPluginVerifier(ide, plugin, problemsFilters, apiUsageFilters, kotlin) {
    it.loadPluginAndVerify()
  }
}