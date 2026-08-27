package mock.plugin.deprecated

open class SamePluginDeprecatedBase {
  @Deprecated(message = "Use newFoo() instead")
  open fun foo() {}
}

/*expected(DEPRECATED)
  Deprecated method mock.plugin.deprecated.SamePluginDeprecatedBase.foo() is overridden

  Deprecated method mock.plugin.deprecated.SamePluginDeprecatedBase.foo() : void is overridden in class mock.plugin.deprecated.SamePluginDeprecatedOverride
*/
class SamePluginDeprecatedOverride : SamePluginDeprecatedBase() {
  override fun foo() {
    // real override logic, not a compiler-generated DefaultImpls-forwarding stub
  }
}
