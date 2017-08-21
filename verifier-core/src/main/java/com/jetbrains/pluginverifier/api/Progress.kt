package com.jetbrains.pluginverifier.api

interface Progress {
  fun getProgress(): Double

  fun setProgress(value: Double)

  fun getText(): String

  fun setText(text: String)
}

data class DefaultProgress(@Volatile private var progress: Double = 0.0,
                           @Volatile private var text: String = "") : Progress {

  override fun getProgress(): Double = progress

  override fun setProgress(value: Double) {
    progress = value
  }

  override fun getText(): String = text

  override fun setText(text: String) {
    this.text = text
  }

}