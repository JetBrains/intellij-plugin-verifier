package com.jetbrains.pluginverifier.progress

//replace with ProgressReporter
interface ProgressIndicator {
  fun getProgress(): Double

  fun setProgress(value: Double)

  fun getText(): String

  fun setText(text: String)
}