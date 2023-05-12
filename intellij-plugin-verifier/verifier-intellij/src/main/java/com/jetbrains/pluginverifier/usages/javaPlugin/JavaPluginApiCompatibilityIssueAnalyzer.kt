package com.jetbrains.pluginverifier.usages.javaPlugin

import com.jetbrains.pluginverifier.verifiers.CompatibilityIssueAnalyzer
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext

/**
 * Requires explicit dependency on the Java plugin via `<depends>com.intellij.modules.java</depends>`.
 *
 * See [Java Functionality extracted as a plugin](https://blog.jetbrains.com/platform/2019/06/java-functionality-extracted-as-a-plugin).
 */
class JavaPluginApiCompatibilityIssueAnalyzer : CompatibilityIssueAnalyzer<JavaPluginClassUsage> {
  override fun analyze(context: PluginVerificationContext, usage: JavaPluginClassUsage) {
    val idePlugin = context.idePlugin
    if (idePlugin.dependencies.none { it.id == "com.intellij.modules.java" || it.id == "com.intellij.java" }) {
      val undeclaredJavaPluginDependencyProblem = context.compatibilityProblems
              .filterIsInstance<UndeclaredDependencyOnJavaPluginProblem>()
              .firstOrNull() ?: UndeclaredDependencyOnJavaPluginProblem().also {
                  context.compatibilityProblems += it
              }
      undeclaredJavaPluginDependencyProblem.javaPluginClassUsages += usage
    }
  }
}