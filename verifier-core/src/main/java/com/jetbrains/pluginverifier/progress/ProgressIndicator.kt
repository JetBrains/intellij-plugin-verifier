package com.jetbrains.pluginverifier.progress

interface ProgressIndicator {
  fun getProgress(): Double

  fun setProgress(value: Double)

  fun getText(): String

  fun setText(text: String)
}