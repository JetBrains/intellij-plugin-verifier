package kotlinDefault

class KotlinDefault {
  fun bar(firstParam: Int = 123, secondParam: String) {
  }

  @JvmOverloads
  fun foo(firstParam: Int = 123, secondParam: String) {
  }
}