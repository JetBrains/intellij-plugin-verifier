package com.jetbrains.plugin.structure.intellij.classes.locator

import com.jetbrains.plugin.structure.classes.resolvers.Resolver

interface LocationKey {
  val name: String

  fun getLocator(readMode: Resolver.ReadMode): ClassesLocator
}