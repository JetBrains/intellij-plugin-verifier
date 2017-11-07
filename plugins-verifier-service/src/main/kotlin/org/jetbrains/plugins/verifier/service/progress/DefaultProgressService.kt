package org.jetbrains.plugins.verifier.service.progress

import org.jetbrains.plugins.verifier.service.service.tasks.ServiceTaskProgress

open class DefaultProgressService : ServiceTaskProgress {

  @Volatile private var progress: Double = 0.0
  @Volatile private var text: String = ""

  override fun getFraction(): Double = progress

  override fun setFraction(value: Double) {
    progress = value
  }

  override fun getText(): String = text

  override fun setText(text: String) {
    this.text = text
  }

}