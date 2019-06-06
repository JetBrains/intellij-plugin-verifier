package com.jetbrains.intellij.feature.extractor

import com.google.gson.Gson
import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.ide.classes.IdeResolverCreator
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import java.io.File

/**
 * Command-line entry point of feature extractor.
 */
fun main(args: Array<String>) {
  if (args.size != 2) {
    throw IllegalArgumentException("Usage: <plugin> <ide>")
  }
  val jsonSerializer = Gson()
  val pluginFile = File(args[0])
  val ideaFile = File(args[1])
  when (val pluginCreationResult = IdePluginManager.createManager().createPlugin(pluginFile)) {
    is PluginCreationSuccess -> {
      val ide = IdeManager.createManager().createIde(ideaFile)
      IdeResolverCreator.createIdeResolver(ide).use { ideResolver ->
        val features = FeaturesExtractor.extractFeatures(ide, ideResolver, pluginCreationResult.plugin)
        for (feature in features) {
          println(jsonSerializer.toJson(feature))
        }
      }
    }
    is PluginCreationFail -> "Plugin is invalid: " + pluginCreationResult.errorsAndWarnings.joinToString()
  }
}