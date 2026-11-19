package mock.plugin.deprecated

import deprecated.DeprecatedDefaultMethodInterface

/*expected(DEPRECATED)
  Deprecated method deprecated.DeprecatedDefaultMethodInterface.foo() is overridden

  Deprecated method deprecated.DeprecatedDefaultMethodInterface.foo() : java.lang.String is overridden in class mock.plugin.deprecated.SuperCallInOverrideOfDeprecatedDefaultMethod
*/
/*expected(DEPRECATED)
  Deprecated method deprecated.DeprecatedDefaultMethodInterface.foo() invocation

  Deprecated method deprecated.DeprecatedDefaultMethodInterface.foo() : java.lang.String is invoked in mock.plugin.deprecated.SuperCallInOverrideOfDeprecatedDefaultMethod.foo() : String
*/
class SuperCallInOverrideOfDeprecatedDefaultMethod : DeprecatedDefaultMethodInterface {
  override fun foo(): String = super.foo() + "!"
}
