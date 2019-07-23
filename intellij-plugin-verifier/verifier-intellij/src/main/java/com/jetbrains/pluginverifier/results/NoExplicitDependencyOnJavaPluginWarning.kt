package com.jetbrains.pluginverifier.results

import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption
import com.jetbrains.pluginverifier.results.presentation.ClassOption
import com.jetbrains.pluginverifier.results.presentation.formatClassLocation
import com.jetbrains.pluginverifier.usages.formatUsageLocation
import com.jetbrains.pluginverifier.usages.javaPlugin.JavaPluginClassUsage

data class NoExplicitDependencyOnJavaPluginWarning(val javaPluginClassUsage: JavaPluginClassUsage) : CompatibilityWarning() {
  override val message: String
    get() = buildString {
      append("Plugin uses class ")
      append("'" + javaPluginClassUsage.usedClass.formatClassLocation(ClassOption.FULL_NAME, ClassGenericsSignatureOption.NO_GENERICS) + "'")
      append(" at ")
      append("'" + javaPluginClassUsage.usageLocation.formatUsageLocation() + "'")
      append(" but does not declare explicit dependency on Java plugin using <depends>com.intellij.modules.java</depends>. ")
      append("Java functionality was extracted from IntelliJ Platform to a separate plugin in IDEA 2019.2. ")
      append("For more info refer to https://blog.jetbrains.com/platform/2019/06/java-functionality-extracted-as-a-plugin")
    }
}