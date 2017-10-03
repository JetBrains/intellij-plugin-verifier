package com.jetbrains.pluginverifier.core

import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.plugin.PluginCreator

/**
 * @author Sergey Patrikeev
 */
object Verification {
  fun run(verifierParams: VerifierParams, pluginCreator: PluginCreator, tasks: List<Pair<PluginCoordinate, IdeDescriptor>>, logger: Progress): List<Result> =
      VerifierExecutor(verifierParams, pluginCreator).use {
        it.verify(tasks, logger)
      }
}