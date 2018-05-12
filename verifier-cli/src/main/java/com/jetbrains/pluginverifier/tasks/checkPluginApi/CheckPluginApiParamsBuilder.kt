package com.jetbrains.pluginverifier.tasks.checkPluginApi

import com.jetbrains.pluginverifier.dependencies.resolution.LastVersionSelector
import com.jetbrains.pluginverifier.dependencies.resolution.PluginVersionSelector
import com.jetbrains.pluginverifier.misc.closeOnException
import com.jetbrains.pluginverifier.misc.exists
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.options.PluginsSet
import com.jetbrains.pluginverifier.parameters.packages.PackageFilter
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.TaskParametersBuilder
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class CheckPluginApiParamsBuilder(private val pluginRepository: PluginRepository,
                                  private val pluginDetailsCache: PluginDetailsCache) : TaskParametersBuilder {
  companion object {
    private const val USAGE = """Expected exactly 3 arguments: <base plugin version> <new plugin version> <plugins to check>.
Example: java -jar verifier.jar check-plugin-api Kotlin-old.zip Kotlin-new.zip kotlin-depends.txt"""
  }

  override fun build(opts: CmdOpts, freeArgs: List<String>): CheckPluginApiParams {
    val apiOpts = CheckPluginApiOpts()
    val args = Args.parse(apiOpts, freeArgs.toTypedArray(), false)

    if (args.size != 3) {
      Args.usage(apiOpts)
      throw IllegalArgumentException(USAGE)
    }

    val basePluginFile = Paths.get(args[0])
    if (!basePluginFile.exists()) {
      throw IllegalArgumentException("Base plugin file $basePluginFile doesn't exist")
    }

    val newPluginFile = Paths.get(args[1])
    if (!newPluginFile.exists()) {
      throw IllegalArgumentException("New plugin file $newPluginFile doesn't exist")
    }

    val pluginsToCheckFile = File(args[2])
    if (!pluginsToCheckFile.exists()) {
      throw IllegalArgumentException("File with list of plugins' IDs to check doesn't exist: $pluginsToCheckFile")
    }

    val pluginsSet = parsePluginsToCheck(pluginsToCheckFile)

    val basePluginPackageFilter = parsePackageFilter(apiOpts.pluginPackages)

    val jdkPath = OptionsParser.getJdkPath(opts)
    val problemsFilters = OptionsParser.getProblemsFilters(opts)

    val basePluginDetails = providePluginDetails(basePluginFile)
    basePluginDetails.closeOnException {
      val newPluginDetails = providePluginDetails(newPluginFile)
      newPluginDetails.closeOnException {
        return CheckPluginApiParams(
            pluginsSet,
            basePluginDetails,
            newPluginDetails,
            jdkPath,
            problemsFilters,
            basePluginPackageFilter
        )
      }
    }
  }

  private fun parsePackageFilter(packages: Array<String>): PackageFilter =
      packages
          .map { it.trim() }
          .mapNotNull {
            val exclude = it.startsWith("-")
            val binaryPackageName = it.trim('+', '-').replace('.', '/')
            if (binaryPackageName.isEmpty()) {
              null
            } else {
              PackageFilter.Descriptor(!exclude, binaryPackageName)
            }
          }
          .let { PackageFilter(it) }


  private fun providePluginDetails(pluginFile: Path) =
      with(pluginDetailsCache.pluginDetailsProvider.providePluginDetails(pluginFile)) {
        when (this) {
          is PluginDetailsProvider.Result.Provided -> pluginDetails
          is PluginDetailsProvider.Result.InvalidPlugin ->
            throw IllegalArgumentException("Plugin $pluginFile is invalid: \n" + pluginErrors.joinToString(separator = "\n") { it.message })
        }
      }

  private fun parsePluginsToCheck(pluginsToCheckFile: File): PluginsSet {
    val pluginsSet = PluginsSet()
    val lastVersionSelector = LastVersionSelector()
    pluginsToCheckFile.readLines().mapNotNull { pluginId ->
      with(lastVersionSelector.selectPluginVersion(pluginId, pluginRepository)) {
        when (this) {
          is PluginVersionSelector.Result.Selected -> pluginInfo
          else -> null
        }
      }
    }.forEach { pluginsSet.schedulePlugin(it) }
    return pluginsSet
  }


}

class CheckPluginApiOpts {

  @set:Argument("plugin-packages", alias = "pp", delimiter = ",", description = "Specifies plugin's packages, classes of which are supposed to be resolved in this plugin.\n" +
      "They are used to detect 'No such class' problems: if a verified plugin references a class belonging to one of the packages and it was not " +
      "resolved in class files of the specific base plugin, it means that the class was removed/moved, leading to binary incompatibility\n" +
      "The list is set up using comma separator. It is possible to exclude an inner package in case it doesn't belong to the base plugin\n" +
      "The syntax for each package is as follows: [+|-]<package>\n" +
      "Example: -plugin-packages org.jetbrains.kotlin,org.kotlin,-org.jetbrains.kotlin.extension")
  var pluginPackages: Array<String> = arrayOf()

}