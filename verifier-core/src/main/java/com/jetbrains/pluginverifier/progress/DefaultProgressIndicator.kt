package com.jetbrains.pluginverifier.progress

open class DefaultProgressIndicator(@Volatile private var progress: Double = 0.0,
                                    @Volatile private var text: String = "") : ProgressIndicator {

  override fun getProgress(): Double = progress

  override fun setProgress(value: Double) {
    progress = value
  }

  override fun getText(): String = text

  override fun setText(text: String) {
    this.text = text
  }

}