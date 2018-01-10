package com.jetbrains.pluginverifier.tasks

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.pluginverifier.repository.PluginInfo

/**
 * Set of [plugins] for the verification and
 * [plugins files] [invalidPluginFiles] that were not parsed
 * because of the plugin structure [errors] [PluginProblem.Level.ERROR].
 *
 * The [invalidPluginFiles] were specified in the verifier parameters,
 * so their [errors] [PluginProblem.Level.ERROR] will be reported.
 */
data class PluginsToCheck(
    /**
     * Plugins to be checked.
     *
     * Those plugins are not necessarily [valid] [com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess]
     * but it is not known until the verification starts.
     */
    val plugins: MutableList<PluginInfo> = arrayListOf(),

    /**
     * [Invalid] [InvalidPluginFile] plugins files
     * specified for the verification but containing
     * the [errors] [InvalidPluginFile.pluginErrors].
     */
    val invalidPluginFiles: MutableList<InvalidPluginFile> = arrayListOf()
)