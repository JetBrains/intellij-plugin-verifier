package kotlinDefault

class KotlinDefault {
  fun bar(firstParam: Int = 123) {
  }

  @JvmOverloads
  fun foo(firstParam: Int = 123) {
  }
}