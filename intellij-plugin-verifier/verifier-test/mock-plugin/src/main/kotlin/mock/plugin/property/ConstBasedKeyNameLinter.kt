@file:Suppress("unused")

package mock.plugin.property

import org.jetbrains.annotations.PropertyKey
import mock.plugin.property.QUIET as Q
import mock.plugin.property.VERBOSE as V

enum class ConstBasedKeyNameLinter(@PropertyKey(resourceBundle = "mock.plugin.property.linter") val titleKey: String) {
  QUIET("$PREFIX.$Q"),
  VERBOSE("$PREFIX.$V")
}

private const val PREFIX = "mock.plugin.linter"
private const val QUIET = "quiet"
private const val VERBOSE = "verbose"