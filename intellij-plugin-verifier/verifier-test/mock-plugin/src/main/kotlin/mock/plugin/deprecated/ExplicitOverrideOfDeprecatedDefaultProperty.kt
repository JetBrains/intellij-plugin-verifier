package mock.plugin.deprecated

import deprecated.DeprecatedDefaultPropertyInterface

/*expected(DEPRECATED)
  Deprecated method deprecated.DeprecatedDefaultPropertyInterface.getProp() is overridden

  Deprecated method deprecated.DeprecatedDefaultPropertyInterface.getProp() : java.lang.String is overridden in class mock.plugin.deprecated.ExplicitOverrideOfDeprecatedDefaultProperty
*/
class ExplicitOverrideOfDeprecatedDefaultProperty : DeprecatedDefaultPropertyInterface {
  override val prop: String
    get() = "overridden"
}
