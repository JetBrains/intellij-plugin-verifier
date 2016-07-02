package com.jetbrains.pluginverifier.commands

import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.IdeManager
import com.intellij.structure.domain.IdeVersion
import com.intellij.structure.domain.Plugin
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.results.ProblemSet
import org.apache.commons.cli.CommandLine
import java.io.File
import java.io.IOException
import java.util.*

/**
 * @author Sergey Evdokimov
 */
abstract class VerifierCommand(val name: String) {

  @Throws(IOException::class)
  protected fun createIde(ideToCheck: File, commandLine: CommandLine): Ide {
    return IdeManager.getInstance().createIde(ideToCheck, takeVersionFromCmd(commandLine))
  }

  /**
   * @return exit code
   */
  @Throws(Exception::class)
  abstract fun execute(commandLine: CommandLine, freeArgs: List<String>): Int

  @Throws(IOException::class)
  protected fun getJdkDir(commandLine: CommandLine): File {
    val runtimeDirectory: File

    if (commandLine.hasOption('r')) {
      runtimeDirectory = File(commandLine.getOptionValue('r'))
      if (!runtimeDirectory.isDirectory) {
        throw RuntimeException("Specified runtime directory is not a directory: " + commandLine.getOptionValue('r'))
      }
    } else {
      val javaHome = System.getenv("JAVA_HOME") ?: throw RuntimeException("JAVA_HOME is not specified")

      runtimeDirectory = File(javaHome)
      if (!runtimeDirectory.isDirectory) {
        throw RuntimeException("Invalid JAVA_HOME: " + javaHome)
      }
    }

    return runtimeDirectory
  }

  @Throws(IOException::class)
  protected fun getExternalClassPath(commandLine: CommandLine): Resolver {
    val values = commandLine.getOptionValues("cp") ?: return Resolver.getEmptyResolver()

    val pools = ArrayList<Resolver>(values.size)

    for (value in values) {
      pools.add(Resolver.createJarResolver(File(value)))
    }

    return Resolver.createUnionResolver("External classpath resolver: " + Arrays.toString(values), pools)
  }

  @Throws(Exception::class)
  protected fun verify(plugin: Plugin,
                       ide: Ide,
                       ideResolver: Resolver,
                       jdkDir: File,
                       externalClassPath: Resolver,
                       options: VOptions): ProblemSet {
    val jdkDescriptor = JdkDescriptor.ByFile(jdkDir)
    val pairs = listOf(Pair<PluginDescriptor, IdeDescriptor>(PluginDescriptor.ByInstance(plugin), IdeDescriptor.ByInstance(ide, ideResolver)))

    //the exceptions are propagated
    val result = VManager.verify(VParams(jdkDescriptor, pairs, options, externalClassPath)).results[0]

    if (result is VResult.Problems) {
      val problemSet = ProblemSet()
      result.problems.entries().forEach { x -> problemSet.addProblem(x.key, x.value) }
      return problemSet
    } else if (result is VResult.BadPlugin) {
      throw IllegalArgumentException(result.overview) //will be caught above
    }
    return ProblemSet()
  }

  @Throws(IOException::class)
  protected fun takeVersionFromCmd(commandLine: CommandLine): IdeVersion? {
    val build = commandLine.getOptionValue("iv")
    if (build != null && !build.isEmpty()) {
      try {
        return IdeVersion.createIdeVersion(build)
      } catch (e: IllegalArgumentException) {
        throw RuntimeException("Incorrect update IDE-version has been specified " + build, e)
      }

    }
    return null
  }

}
