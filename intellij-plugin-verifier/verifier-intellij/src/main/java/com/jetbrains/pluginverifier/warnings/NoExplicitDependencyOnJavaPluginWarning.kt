package com.jetbrains.pluginverifier.warnings

import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption
import com.jetbrains.pluginverifier.results.presentation.ClassOption
import com.jetbrains.pluginverifier.results.presentation.formatClassLocation
import com.jetbrains.pluginverifier.usages.formatUsageLocation
import com.jetbrains.pluginverifier.usages.javaPlugin.JavaPluginClassUsage

data class NoExplicitDependencyOnJavaPluginWarning(
  val javaPluginClassUsages: MutableSet<JavaPluginClassUsage> = hashSetOf()
) : CompatibilityWarning() {

  override val shortDescription
    get() = "Dependency on Java plugin is not specified"

  override val fullDescription
    get() = buildString {
      appendln("Plugin uses classes of Java plugin, for example")
      val deterministicUsages = javaPluginClassUsages.sortedWith(compareBy<JavaPluginClassUsage> { it.usedClass.className }.thenBy { it.usageLocation.presentableLocation })
      deterministicUsages.take(3).forEach { usage ->
        append("'${usage.usedClass.formatClassLocation(ClassOption.FULL_NAME, ClassGenericsSignatureOption.NO_GENERICS)}'")
        append(" is used at ")
        append("'${usage.usageLocation.formatUsageLocation()}'")
        appendln()
      }
      appendln("but the plugin does not declare explicit dependency on the Java plugin, via <depends>com.intellij.modules.java</depends>. ")
      appendln("Java functionality was extracted from the IntelliJ Platform to a separate plugin in IDEA 2019.2. ")
      appendln("For more info refer to https://blog.jetbrains.com/platform/2019/06/java-functionality-extracted-as-a-plugin")
    }
}