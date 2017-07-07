package org.jetbrains.plugins.verifier.service.progress

/**
 * @author Sergey Patrikeev
 */
interface TaskProgress {

  fun getProgress(): Double

  fun setProgress(value: Double)

  fun getText(): String

  fun setText(text: String)

}