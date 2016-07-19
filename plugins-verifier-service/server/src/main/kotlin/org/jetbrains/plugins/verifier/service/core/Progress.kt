package org.jetbrains.plugins.verifier.service.core

import com.jetbrains.pluginverifier.api.VProgress

/**
 * @author Sergey Patrikeev
 */
interface Progress {

  fun getProgress(): Double

  fun setProgress(value: Double)

  fun getText(): String

  fun setText(text: String)

}

class DefaultProgress() : Progress {

  @Volatile private var progress: Double = 0.0
  @Volatile private var text: String = ""

  override fun getProgress(): Double = progress

  override fun setProgress(value: Double) {
    progress = value
  }

  override fun getText(): String = text

  override fun setText(text: String) {
    this.text = text
  }

}

class BridgeVProgress(val progress: Progress) : VProgress {
  override fun getProgress(): Double = progress.getProgress()

  override fun getText(): String = progress.getText()

  override fun setProgress(value: Double) {
    progress.setProgress(value)
  }

  override fun setText(text: String) {
    progress.setText(text)
  }

}