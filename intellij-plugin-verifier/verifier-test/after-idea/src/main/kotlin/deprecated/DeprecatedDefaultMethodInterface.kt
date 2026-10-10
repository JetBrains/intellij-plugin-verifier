package deprecated

interface DeprecatedDefaultMethodInterface {
  @Deprecated(message = "No longer available in the after-idea")
  fun foo(): String = "default"
}
