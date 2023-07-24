package internal.defaultMethod

interface InterfaceWithDefaultMethodUsingInternalAPI : TopInterfaceWithDefaultMethodUsingInternalAPI {

  val id: String

  fun returningInternal() : AnInternalType? = null
  fun internalArgsReturningInternal(anInternalType: AnInternalType, s: String) : AnInternalType? = null
  fun internalArgsReturningVoid(anInternalType: AnInternalType, s: String) {}
  fun internalArgsAndPrimitiveArgs(anInternalType: AnInternalType, i: Int, b: Boolean, ui: UInt, objectArray: Array<String>, intArray: Array<Int>) {}
}