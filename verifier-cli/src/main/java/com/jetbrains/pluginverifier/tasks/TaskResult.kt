package com.jetbrains.pluginverifier.tasks

/**
 * Base class of all the verification [tasks] [Task]' results.
 */
abstract class TaskResult(
    /**
     * List of [invalid] [com.jetbrains.plugin.structure.base.plugin.PluginCreationFail]
     * local plugins which problems must be reported in the final results.
     */
    val invalidPluginFiles: List<InvalidPluginFile>
)