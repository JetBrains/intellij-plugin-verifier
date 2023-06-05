package internal.defaultMethod

interface InterfaceWithDefaultMethodUsingInternalAPI : TopInterfaceWithDefaultMethodUsingInternalAPI {
  val id: String

  fun returningInternal() : AnInternalType? = null
  fun internalArgsReturningInternal(anInternalType: AnInternalType, s: String) : AnInternalType? = null
  fun internalArgsReturningVoid(anInternalType: AnInternalType, s: String) {}
}