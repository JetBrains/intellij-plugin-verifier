package com.jetbrains.plugin.structure.intellij.classes.locator

/**
 * @author Sergey Patrikeev
 */
interface LocationKey {
  val name: String

  val locator: ClassesLocator
}