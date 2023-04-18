package internal.defaultMethod

interface InterfaceWithDefaultMethodUsingInternalAPI {

  val id: String

  fun getPlaceholderCollector() : AnInternalType? = null
}