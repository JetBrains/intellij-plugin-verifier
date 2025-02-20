package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PluginProblem.Level.ERROR
import com.jetbrains.plugin.structure.intellij.plugin.ValidationContext.ValidationResult.*
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.verifiers.ProblemRegistrar
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger(PluginBeanValidator::class.java)

class ValidationContext(val descriptorPath: String, val problemResolver: PluginCreationResultResolver) : ProblemRegistrar {
  private val _problems = mutableListOf<PluginProblem>()

  val problems: List<PluginProblem>
    get() = _problems

  override fun registerProblem(problem: PluginProblem) {
    _problems += problem
  }

  operator fun plusAssign(problem: PluginProblem) {
    registerProblem(problem)
  }

  fun getResult(invalidPluginProvider: () -> InvalidPlugin): ValidationResult {
    if (problems.isEmpty()) {
      return Valid
    }
    val invalidPlugin = invalidPluginProvider()
    val remappedProblems = problemResolver.classify(invalidPlugin, problems)
    return when {
      remappedProblems.isEmpty() -> Valid
      remappedProblems.hasErrors() -> Invalid(invalidPlugin, remappedProblems)
      else -> ValidWithWarnings(remappedProblems)
    }
  }

  private fun List<PluginProblem>.hasErrors(): Boolean = any { it.level == ERROR }

  private fun List<PluginProblem>.notEmpty(pluginId: String): Boolean {
    return if (isEmpty()) {
      false
    } else {
      if (LOG.isDebugEnabled) {
        val errorMsg = joinToString()
        LOG.debug("Plugin '$pluginId' has $size error(s): $errorMsg")
      }
      true
    }
  }

  sealed class ValidationResult {
    object Valid : ValidationResult()
    class ValidWithWarnings(val warnings: List<PluginProblem>) : ValidationResult()
    data class Invalid(val invalidPlugin: InvalidPlugin, val problems: List<PluginProblem>) : ValidationResult() {

    }
  }
}
