package kotlinDefault

class KotlinDefault {
  @Suppress("UNUSED_PARAMETER")
  fun bar(firstParam: Int = 123) {
  }

  @Suppress("UNUSED_PARAMETER")
  @JvmOverloads
  fun foo(firstParam: Int = 123) {
  }
}