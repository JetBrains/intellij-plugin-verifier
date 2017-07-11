package com.jetbrains.intellij.feature.extractor

import com.google.gson.Gson
import com.intellij.structure.ide.IdeManager
import com.intellij.structure.plugin.PluginCreationFail
import com.intellij.structure.plugin.PluginCreationSuccess
import com.intellij.structure.plugin.PluginManager
import com.intellij.structure.resolvers.Resolver
import java.io.File

/**
 * Command-line entry point of feature extractor.
 */
fun main(args: Array<String>) {
  if (args.size != 2) {
    throw IllegalArgumentException("Usage: <plugin> <idea>")
  }
  val pluginFile = File(args[0])
  val ideaFile = File(args[1])
  val pluginCreationResult = PluginManager.getInstance().createPlugin(pluginFile)
  when (pluginCreationResult) {
    is PluginCreationSuccess -> {
      val ide = IdeManager.getInstance().createIde(ideaFile)
      Resolver.createIdeResolver(ide).use { ideResolver ->
        val extractorResult = FeaturesExtractor.extractFeatures(ide, ideResolver, pluginCreationResult.plugin)
        extractorResult.features.forEach { println(Gson().toJson(it)) }
        println("All features extracted: ${extractorResult.extractedAll}")
      }
    }
    is PluginCreationFail -> println("Plugin is invalid: " + pluginCreationResult.errorsAndWarnings.joinToString())
  }
}