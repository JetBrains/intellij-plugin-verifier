package com.jetbrains.intellij.feature.extractor

import com.google.gson.Gson
import com.intellij.structure.domain.IdeManager
import com.intellij.structure.domain.PluginManager
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
  val plugin = PluginManager.getInstance().createPlugin(pluginFile, false)
  val ide = IdeManager.getInstance().createIde(ideaFile)
  val extractorResult = FeaturesExtractor.extractFeatures(ide, plugin)
  extractorResult.features.forEach { println(Gson().toJson(it)) }
  println("All features extracted: ${extractorResult.extractedAll}")
}