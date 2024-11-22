package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.classes.locator.ClassesLocator
import com.jetbrains.plugin.structure.intellij.classes.locator.LocationKey

class MockLocationKey(override val name: String, val classesLocator: ClassesLocator) : LocationKey {
  override fun getLocator(readMode: Resolver.ReadMode) = classesLocator
}