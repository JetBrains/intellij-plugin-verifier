package com.jetbrains.intellij.feature.extractor

import com.google.gson.Gson
import com.intellij.structure.ide.IdeManager
import com.intellij.structure.plugin.PluginCreationFail
import com.intellij.structure.plugin.PluginCreationSuccess
import com.intellij.structure.plugin.PluginManager
import java.io.File

/**
 * @author Sergey Patrikeev
 */
fun main(args: Array<String>) {
  if (args.size != 2) {
    throw IllegalArgumentException("Usage: <plugin> <idea>")
  }
  val pluginFile = File(args[0])
  val ideaFile = File(args[1])
  val pluginCreationResult = PluginManager.getInstance().createPlugin(pluginFile)
  if (pluginCreationResult.isSuccess) {
    val ide = IdeManager.getInstance().createIde(ideaFile)
    val extractorResult = FeaturesExtractor.extractFeatures(ide, (pluginCreationResult as PluginCreationSuccess).plugin)
    extractorResult.features.forEach { println(Gson().toJson(it)) }
    println("All features extracted: ${extractorResult.extractedAll}")
  } else {
    println("Plugin is invalid: " + (pluginCreationResult as PluginCreationFail).errorsAndWarnings.joinToString())
  }
}