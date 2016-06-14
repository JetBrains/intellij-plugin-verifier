package com.jetbrains.pluginverifier

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.intellij.structure.domain.Plugin
import com.intellij.structure.impl.utils.StringUtil
import com.jetbrains.pluginverifier.misc.RepositoryConfiguration
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.utils.Pair
import com.jetbrains.pluginverifier.utils.Util
import org.apache.commons.cli.CommandLine
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.*
import java.util.regex.Pattern

/**
 * @author Sergey Patrikeev
 */
interface VOptions {

  fun isIgnoredProblem(plugin: Plugin, problem: Problem): Boolean

  fun isIgnoreDependency(pluginId: String): Boolean

  fun isExternalClass(className: String): Boolean

  companion object {
    fun parseOpts(commandLine: CommandLine): VOptions {
      return VOptionsImpl.Companion.parseOpts(commandLine)
    }
  }

}


class VOptionsImpl constructor(val prefixesToSkipForDuplicateClassesCheck: Array<String>,
                               val externalClassPrefixes: Array<String>,
                               val optionalDependenciesIdsToIgnoreIfMissing: Set<String>,
                               /**
                                             * Map of _(pluginXmlId, version)_ -> to be ignored _problem pattern_
                                             */
                               private val myProblemsToIgnore: Multimap<Pair<String, String>, Pattern>) : VOptions {
  override fun isIgnoredProblem(plugin: Plugin, problem: Problem): Boolean {
    val xmlId = plugin.pluginId
    val version = plugin.pluginVersion
    for (entry in myProblemsToIgnore.entries()) {
      val ignoreXmlId = entry.key.getFirst()
      val ignoreVersion = entry.key.getSecond()
      val ignoredPattern = entry.value

      if (StringUtil.equal(xmlId, ignoreXmlId)) {
        if (StringUtil.isEmpty(ignoreVersion) || StringUtil.equal(version, ignoreVersion)) {
          if (ignoredPattern.matcher(problem.description.replace('/', '.')).matches()) {
            return true
          }
        }
      }
    }
    return false
  }

  override fun isIgnoreDependency(pluginId: String): Boolean {
    return isIgnoreMissingOptionalDependency(pluginId) //TODO: add an option to ignore mandatory plugins too
  }

  private fun isIgnoreMissingOptionalDependency(pluginId: String): Boolean {
    return optionalDependenciesIdsToIgnoreIfMissing.contains(pluginId)
  }

  override fun isExternalClass(className: String): Boolean {
    for (prefix in externalClassPrefixes) {
      if (prefix.length > 0 && className.startsWith(prefix)) {
        return true
      }
    }
    return false
  }

  companion object {

    private fun getOption(commandLine: CommandLine, shortKey: String): String? {
      val option = Util.CMD_OPTIONS.getOption(shortKey)

      val cmdValue = commandLine.getOptionValue(shortKey)
      if (cmdValue != null) return cmdValue

      return RepositoryConfiguration.getInstance().getProperty(option.longOpt)
    }

    private fun getOptionValues(commandLine: CommandLine, shortKey: String): List<String> {
      val res = ArrayList<String>()

      val cmdValues = commandLine.getOptionValues(shortKey)
      if (cmdValues != null) {
        Collections.addAll(res, *cmdValues)
      }

      val option = Util.CMD_OPTIONS.getOption(shortKey)
      val cfgProperty = RepositoryConfiguration.getInstance().getProperty(option.longOpt)

      if (cfgProperty != null) {
        res.add(cfgProperty)
      }

      return res
    }

    private fun getOptionValuesSplit(commandLine: CommandLine, splitter: String, shortKey: String): Array<String> {
      val res = ArrayList<String>()
      for (optionStr in getOptionValues(commandLine, shortKey)) {
        if (optionStr.isEmpty()) continue

        Collections.addAll(res, *optionStr.split(splitter.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
      }

      return res.toTypedArray()
    }

    fun parseOpts(commandLine: CommandLine): VOptionsImpl {
      val prefixesToSkipForDuplicateClassesCheck = getOptionValuesSplit(commandLine, ":", "s")
      for (i in prefixesToSkipForDuplicateClassesCheck.indices) {
        prefixesToSkipForDuplicateClassesCheck[i] = prefixesToSkipForDuplicateClassesCheck[i].replace('.', '/')
      }

      val externalClasses = getOptionValuesSplit(commandLine, ":", "e")
      for (i in externalClasses.indices) {
        externalClasses[i] = externalClasses[i].replace('.', '/')
      }
      val optionalDependenciesIdsToIgnoreIfMissing: Set<String> = HashSet(getOptionValuesSplit(commandLine, ",", "imod").toList())

      var problemsToIgnore: Multimap<Pair<String, String>, Pattern> = HashMultimap.create<Pair<String, String>, Pattern>()

      val ignoreProblemsFile = getOption(commandLine, "ip")
      if (ignoreProblemsFile != null) {
        problemsToIgnore = getProblemsToIgnoreFromFile(ignoreProblemsFile)
      }

      return VOptionsImpl(prefixesToSkipForDuplicateClassesCheck, externalClasses, optionalDependenciesIdsToIgnoreIfMissing, problemsToIgnore)
    }

    private fun getProblemsToIgnoreFromFile(ignoreProblemsFile: String): Multimap<Pair<String, String>, Pattern> {
      val file = File(ignoreProblemsFile)
      if (!file.exists()) {
        throw IllegalArgumentException("Ignored problems file doesn't exist " + ignoreProblemsFile)
      }

      val m = HashMultimap.create<Pair<String, String>, Pattern>()
      try {
        BufferedReader(FileReader(file)).use { br ->
          var s: String
          while (true) {
            s = br.readLine() ?: break
            s = s.trim { it <= ' ' }
            if (s.isEmpty() || s.startsWith("//")) continue //it is a comment

            val tokens = s.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            if (tokens.size != 3) {
              throw IllegalArgumentException("incorrect problem line $s\nthe line must be in the form: <plugin_xml_id>:<plugin_version>:<problem_description_regexp_pattern>\n<plugin_version> may be empty (which means that a problem will be ignored in all the versions of the plugin)\nexample 'org.jetbrains.kotlin::accessing to unknown class org/jetbrains/kotlin/compiler/.*' - ignore all the missing classes from org.jetbrains.kotlin.compiler package")
            }

            val pluginId = tokens[0].trim { it <= ' ' }
            val pluginVersion = tokens[1].trim { it <= ' ' }
            val ignorePattern = tokens[2].trim { it <= ' ' }.replace('/', '.')

            m.put(Pair.create(pluginId, pluginVersion), Pattern.compile(ignorePattern))
          }
        }
      } catch (e: Exception) {
        throw RuntimeException("Unable to parse ignored problems file " + ignoreProblemsFile, e)
      }

      return m
    }
  }


}