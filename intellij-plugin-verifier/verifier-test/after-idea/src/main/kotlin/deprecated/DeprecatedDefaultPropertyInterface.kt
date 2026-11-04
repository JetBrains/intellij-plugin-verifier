package deprecated

interface DeprecatedDefaultPropertyInterface {
  @Deprecated(message = "No longer available in the after-idea")
  val prop: String
    get() = "default"
}
