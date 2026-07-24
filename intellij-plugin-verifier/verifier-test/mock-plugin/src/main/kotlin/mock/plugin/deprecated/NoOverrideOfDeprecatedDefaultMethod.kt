package mock.plugin.deprecated

import deprecated.DeprecatedDefaultMethodInterface

// The Kotlin compiler generates, in this class, a stub override of foo() that only forwards to
// DeprecatedDefaultMethodInterface$DefaultImpls.foo(this). Neither that forwarding call nor the
// stub override itself were written by the plugin author, so no deprecation warning is expected.
class NoOverrideOfDeprecatedDefaultMethod : DeprecatedDefaultMethodInterface
