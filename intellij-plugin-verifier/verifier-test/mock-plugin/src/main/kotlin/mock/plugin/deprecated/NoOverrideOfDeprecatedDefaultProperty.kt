package mock.plugin.deprecated

import deprecated.DeprecatedDefaultPropertyInterface

// The Kotlin compiler synthesizes a bridge for the inherited getProp() here, just like for an
// inherited default function, so no deprecation warning is expected.
class NoOverrideOfDeprecatedDefaultProperty : DeprecatedDefaultPropertyInterface
