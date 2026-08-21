package mock.plugin.deprecated

import deprecated.DeprecatedDefaultMethodInterface

/*expected(DEPRECATED)
  Deprecated method deprecated.DeprecatedDefaultMethodInterface.foo() is overridden

  Deprecated method deprecated.DeprecatedDefaultMethodInterface.foo() : java.lang.String is overridden in class mock.plugin.deprecated.ExplicitOverrideOfDeprecatedDefaultMethod
*/
class ExplicitOverrideOfDeprecatedDefaultMethod : DeprecatedDefaultMethodInterface {
  override fun foo(): String = "overridden"
}
