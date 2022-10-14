package internal.intellijInternalApi

import com.intellij.openapi.util.IntellijInternalApi

@IntellijInternalApi
class IntellijInternalApiClass {
  var x = 0
  fun foo() {}

  companion object {
    var staticField: String? = null
    fun staticFun() {}
  }
}