/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks.checkPluginApi

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.readLines
import com.jetbrains.plugin.structure.intellij.plugin.PluginArchiveManager
import com.jetbrains.plugin.structure.intellij.plugin.createIdePluginManager
import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.jdk.JdkDescriptorCreator
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.options.PluginsParsing
import com.jetbrains.pluginverifier.options.PluginsSet
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.repositories.local.LocalPluginInfo
import com.jetbrains.pluginverifier.resolution.PluginApiClassResolverProvider
import com.jetbrains.pluginverifier.tasks.TaskParametersBuilder
import com.jetbrains.pluginverifier.verifiers.packages.DefaultPackageFilter
import com.jetbrains.pluginverifier.verifiers.packages.PackageFilter
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths

class CheckPluginApiParamsBuilder(
  private val pluginRepository: PluginRepository,
  private val pluginDetailsCache: PluginDetailsCache,
  private val archiveManager: PluginArchiveManager,
  private val reportage: PluginVerificationReportage
) : TaskParametersBuilder {
  private companion object {
    const val USAGE = """Expected exactly 3 arguments: <base plugin version> <new plugin version> <plugins to check>.
Example: java -jar verifier.jar check-plugin-api Kotlin-old.zip Kotlin-new.zip kotlin-depends.txt"""
  }

  private val pluginManager = createIdePluginManager(archiveManager)

  override fun build(opts: CmdOpts, freeArgs: List<String>): CheckPluginApiParams {
    val apiOpts = CheckPluginApiOpts()
    val args = Args.parse(apiOpts, freeArgs.toTypedArray(), false)

    if (args.size != 3) {
      Args.usage(apiOpts)
      throw IllegalArgumentException(USAGE)
    }

    val basePluginFile = Paths.get(args[0])
    require(basePluginFile.exists()) { "Base plugin file $basePluginFile doesn't exist" }

    val newPluginFile = Paths.get(args[1])
    require(newPluginFile.exists()) { "New plugin file $newPluginFile doesn't exist" }

    val pluginsToCheckFile = Paths.get(args[2])
    require(pluginsToCheckFile.exists()) { "File with list of plugins' IDs to check doesn't exist: $pluginsToCheckFile" }

    val pluginsSet = parsePluginsToCheck(pluginsToCheckFile)

    val pluginPackageFilter = parsePackageFilter(apiOpts.pluginPackages)

    val jdkPath = requireNotNull(opts.runtimeDir?.let { Paths.get(it) }) { "JDK runtime is not specified" }
    val problemsFilters = OptionsParser.getProblemsFilters(opts)

    val basePluginDetails = providePluginDetails(basePluginFile)
    basePluginDetails.closeOnException {
      val newPluginDetails = providePluginDetails(newPluginFile)
      newPluginDetails.closeOnException {

        val jdkDescriptor = JdkDescriptorCreator.createJdkDescriptor(jdkPath)
        jdkDescriptor.closeOnException {
          val baseClassResolverProvider = PluginApiClassResolverProvider(jdkDescriptor, basePluginDetails, pluginPackageFilter)
          val baseVerificationDescriptors = pluginsSet.pluginsToCheck.map {
            PluginVerificationDescriptor.Plugin(it, basePluginDetails.pluginInfo, baseClassResolverProvider, jdkDescriptor.jdkVersion)
          }

          val newClassResolverProvider = PluginApiClassResolverProvider(jdkDescriptor, newPluginDetails, pluginPackageFilter)
          val newVerificationDescriptors = pluginsSet.pluginsToCheck.map {
            PluginVerificationDescriptor.Plugin(it, newPluginDetails.pluginInfo, newClassResolverProvider, jdkDescriptor.jdkVersion)
          }

          val baseVerificationTarget = PluginVerificationTarget.Plugin(basePluginDetails.pluginInfo, jdkDescriptor.jdkVersion)
          val newVerificationTarget = PluginVerificationTarget.Plugin(newPluginDetails.pluginInfo, jdkDescriptor.jdkVersion)

          return CheckPluginApiParams(
            basePluginDetails,
            newPluginDetails,
            jdkDescriptor,
            problemsFilters,
            baseVerificationDescriptors,
            newVerificationDescriptors,
            baseVerificationTarget,
            newVerificationTarget,
            opts.excludeExternalBuildClassesSelector
          )
        }
      }
    }
  }

  private fun parsePackageFilter(packages: Array<String>): PackageFilter =
    DefaultPackageFilter(
      packages
        .map { it.trim() }
        .mapNotNull {
          val exclude = it.startsWith("-")
          val binaryPackageName = it.trim('+', '-').replace('.', '/')
          if (binaryPackageName.isEmpty()) {
            null
          } else {
            DefaultPackageFilter.Descriptor(!exclude, binaryPackageName)
          }
        }
    )


  private fun providePluginDetails(pluginFile: Path): PluginDetails {
    val pluginCreationResult = pluginManager.createPlugin(pluginFile)
    check(pluginCreationResult is PluginCreationSuccess) { pluginCreationResult.toString() }
    val localPluginInfo = LocalPluginInfo(pluginCreationResult.plugin)
    val cacheEntry = pluginDetailsCache.getPluginDetailsCacheEntry(localPluginInfo)
    check(cacheEntry is PluginDetailsCache.Result.Provided)
    return cacheEntry.pluginDetails
  }

  /**
   * Parses [pluginsToCheckFile] for a [PluginsSet].
   *
   * Lines can be:
   * - `<plugin-id>` - adds the last version of <plugin-id>
   * - `<plugin-path>` - adds plugin from local path <plugin-path>.
   * If the <plugin-path> is followed by "!!", this plugin's descriptor
   * will not be verified. It can be used to specify a bundled plugin,
   * which has some mandatory elements missing (like <idea-version>).
   */
  private fun parsePluginsToCheck(pluginsToCheckFile: Path): PluginsSet {
    val pluginsSet = PluginsSet()
    val pluginsParsing = PluginsParsing(pluginRepository, archiveManager, reportage, pluginsSet)

    for (line in pluginsToCheckFile.readLines()) {
      val validateDescriptor = !line.endsWith("!!")
      val path = line.substringBeforeLast("!!")

      val pluginPath = try {
        Paths.get(path).takeIf { it.exists() }
          ?: pluginsToCheckFile.resolveSibling(path).takeIf { it.exists() }
      } catch (e: InvalidPathException) {
        null
      }

      if (pluginPath != null) {
        pluginsParsing.addPluginFile(pluginPath, validateDescriptor)
      } else {
        pluginsParsing.addLastPluginVersion(line)
      }
    }

    return pluginsSet
  }


}

class CheckPluginApiOpts {

  @set:Argument(
    "plugin-packages", alias = "pp", delimiter = ",", description = "Specifies plugin's packages, classes of which are supposed to be resolved in this plugin.\n" +
    "They are used to detect 'No such class' problems: if a verified plugin references a class belonging to one of the packages and it was not " +
    "resolved in class files of the specific base plugin, it means that the class was removed/moved, leading to binary incompatibility\n" +
    "The list is set up using comma separator. It is possible to exclude an inner package in case it doesn't belong to the base plugin\n" +
    "The syntax for each package is as follows: [+|-]<package>\n" +
    "Example: -plugin-packages org.jetbrains.kotlin,org.kotlin,-org.jetbrains.kotlin.extension"
  )
  var pluginPackages: Array<String> = arrayOf()

}