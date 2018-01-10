package com.jetbrains.pluginverifier.tasks

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import java.nio.file.Path

/**
 * Descriptor of a plugin which was specified for the verification
 * but which has structure [errors] [pluginErrors].
 */
data class InvalidPluginFile(val pluginFile: Path, val pluginErrors: List<PluginProblem>)