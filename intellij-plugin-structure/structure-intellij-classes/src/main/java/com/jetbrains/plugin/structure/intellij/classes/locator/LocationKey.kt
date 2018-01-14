package com.jetbrains.plugin.structure.intellij.classes.locator

interface LocationKey {
  val name: String

  val locator: ClassesLocator
}