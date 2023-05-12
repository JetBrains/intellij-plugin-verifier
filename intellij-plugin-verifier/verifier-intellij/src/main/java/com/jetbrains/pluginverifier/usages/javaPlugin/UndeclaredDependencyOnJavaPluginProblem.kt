package com.jetbrains.pluginverifier.usages.javaPlugin

import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption
import com.jetbrains.pluginverifier.results.presentation.ClassOption
import com.jetbrains.pluginverifier.results.presentation.formatClassLocation
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.usages.formatUsageLocation
import java.util.*

class UndeclaredDependencyOnJavaPluginProblem(
  val javaPluginClassUsages: MutableSet<JavaPluginClassUsage> = hashSetOf()
) : CompatibilityProblem() {
  override val problemType: String
    get() = shortDescription

  override val shortDescription: String
    get() = "Dependency on Java plugin is not specified"

  override val fullDescription: String
    get() = buildString {
      appendLine("Plugin uses classes of Java plugin, for example")
      val deterministicUsages = javaPluginClassUsages.sortedWith(compareBy<JavaPluginClassUsage> { it.usedClass.className }.thenBy { it.usageLocation.presentableLocation })
      deterministicUsages.take(3).forEach { usage ->
        append("'${usage.usedClass.formatClassLocation(ClassOption.FULL_NAME, ClassGenericsSignatureOption.NO_GENERICS)}'")
        append(" is used at ")
        append("'${usage.usageLocation.formatUsageLocation()}'")
        appendLine()
      }
      appendLine("but the plugin does not declare explicit dependency on the Java plugin, via <depends>com.intellij.modules.java</depends>. ")
      appendLine("Java functionality was extracted from the IntelliJ Platform to a separate plugin in IDEA 2019.2. ")
      appendLine("For more info refer to https://blog.jetbrains.com/platform/2019/06/java-functionality-extracted-as-a-plugin")
    }

  override fun equals(other: Any?) = other is UndeclaredDependencyOnJavaPluginProblem
          && javaPluginClassUsages == other.javaPluginClassUsages

  override fun hashCode() = Objects.hash(javaPluginClassUsages)
}
