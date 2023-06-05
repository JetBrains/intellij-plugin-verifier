package internal.defaultMethod

interface TopInterfaceWithDefaultMethodUsingInternalAPI {
  fun topInternal(): AnInternalType? = object: AnInternalType {}
}