package org.jetbrains.plugins.verifier.service.progress

import com.jetbrains.pluginverifier.api.Progress

class BridgeVerifierProgress(val progress: TaskProgress) : Progress {
  override fun getProgress(): Double = progress.getProgress()

  override fun getText(): String = progress.getText()

  override fun setProgress(value: Double) {
    progress.setProgress(value)
  }

  override fun setText(text: String) {
    progress.setText(text)
  }

}