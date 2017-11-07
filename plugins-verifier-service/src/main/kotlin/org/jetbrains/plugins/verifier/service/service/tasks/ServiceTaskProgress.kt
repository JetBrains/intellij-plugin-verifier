package org.jetbrains.plugins.verifier.service.service.tasks

/**
 * @author Sergey Patrikeev
 */
interface ServiceTaskProgress {

  fun getFraction(): Double

  fun setFraction(value: Double)

  fun getText(): String

  fun setText(text: String)

}