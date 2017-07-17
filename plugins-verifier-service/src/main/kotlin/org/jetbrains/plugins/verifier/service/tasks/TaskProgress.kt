package org.jetbrains.plugins.verifier.service.tasks

/**
 * @author Sergey Patrikeev
 */
interface TaskProgress {

  fun getFraction(): Double

  fun setFraction(value: Double)

  fun getText(): String

  fun setText(text: String)

}