package org.jetbrains.plugins.verifier.service.progress

import com.jetbrains.pluginverifier.api.Progress
import org.jetbrains.plugins.verifier.service.tasks.TaskProgress

class VerifierToTaskBridgeProgress(val taskProgress: TaskProgress) : Progress {
  override fun getProgress(): Double = taskProgress.getFraction()

  override fun getText(): String = taskProgress.getText()

  override fun setProgress(value: Double) {
    taskProgress.setFraction(value)
  }

  override fun setText(text: String) {
    taskProgress.setText(text)
  }

}