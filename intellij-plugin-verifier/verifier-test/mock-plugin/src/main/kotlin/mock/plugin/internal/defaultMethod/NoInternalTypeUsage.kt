package mock.plugin.internal.defaultMethod

import internal.defaultMethod.InterfaceWithDefaultMethodUsingInternalAPI

class NoInternalTypeUsage : InterfaceWithDefaultMethodUsingInternalAPI {
  override val id: String = ""

  // as plugin developer, do not override the default implementation of getPlaceholderCollector()
  // as it does use an internal type

  fun zeroInstructionsMethod() {}
}