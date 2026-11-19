package mock.plugin.deprecated

import deprecated.DeprecatedDefaultMethodInterface

// This override compiles to the same bytecode as the bridge the Kotlin compiler synthesizes in
// NoOverrideOfDeprecatedDefaultMethod, but it is declared in source, so both the override and the
// super call must still be reported.
/*expected(DEPRECATED)
  Deprecated method deprecated.DeprecatedDefaultMethodInterface.foo() is overridden

  Deprecated method deprecated.DeprecatedDefaultMethodInterface.foo() : java.lang.String is overridden in class mock.plugin.deprecated.TrivialOverrideOfDeprecatedDefaultMethod
*/
/*expected(DEPRECATED)
  Deprecated method deprecated.DeprecatedDefaultMethodInterface.foo() invocation

  Deprecated method deprecated.DeprecatedDefaultMethodInterface.foo() : java.lang.String is invoked in mock.plugin.deprecated.TrivialOverrideOfDeprecatedDefaultMethod.foo() : String
*/
class TrivialOverrideOfDeprecatedDefaultMethod : DeprecatedDefaultMethodInterface {
  override fun foo(): String = super.foo()
}
