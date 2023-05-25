/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.intellij.feature.extractor

import com.fasterxml.jackson.databind.ObjectMapper
import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.ide.classes.IdeResolverCreator
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import java.nio.file.Paths

/**
 * Command-line entry point of feature extractor.
 */
fun main(args: Array<String>) {
  require(args.size == 2) { "Usage: <plugin> <ide>" }
  val jsonSerializer = ObjectMapper()
  val pluginFile = Paths.get(args[0])
  val ideaFile = Paths.get(args[1])
  when (val pluginCreationResult = IdePluginManager.createManager().createPlugin(pluginFile)) {
    is PluginCreationSuccess -> {
      val ide = IdeManager.createManager().createIde(ideaFile)
      IdeResolverCreator.createIdeResolver(ide).use { ideResolver ->
        val features = FeaturesExtractor.extractFeatures(ide, ideResolver, pluginCreationResult.plugin)
        for (feature in features) {
          println(jsonSerializer.writeValueAsString(feature))
        }
      }
    }
    is PluginCreationFail -> println("Plugin is invalid: " + pluginCreationResult.errorsAndWarnings.joinToString())
  }
}