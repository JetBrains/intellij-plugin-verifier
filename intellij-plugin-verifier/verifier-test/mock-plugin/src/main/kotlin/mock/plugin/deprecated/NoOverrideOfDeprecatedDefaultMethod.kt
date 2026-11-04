package mock.plugin.deprecated

import deprecated.DeprecatedDefaultMethodInterface

// The Kotlin compiler synthesizes, in this class, a bridge for foo() that only forwards to the
// interface's default implementation (`INVOKESPECIAL DeprecatedDefaultMethodInterface.foo()` or
// `INVOKESTATIC DeprecatedDefaultMethodInterface$DefaultImpls.foo(this)`, depending on the
// `-jvm-default` mode). Neither that bridge nor its forwarding call were written by the plugin
// author, so no deprecation warning is expected.
class NoOverrideOfDeprecatedDefaultMethod : DeprecatedDefaultMethodInterface
