package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.tasks.TaskResult
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedApiUsage

class DeprecatedUsagesResult(
    val verifiedIdeVersion: IdeVersion,
    val ideVersionForCompatiblePlugins: IdeVersion,
    val pluginDeprecatedUsages: Map<PluginInfo, Set<DeprecatedApiUsage>>,
    val deprecatedIdeApiElements: Set<Location>
) : TaskResult()
