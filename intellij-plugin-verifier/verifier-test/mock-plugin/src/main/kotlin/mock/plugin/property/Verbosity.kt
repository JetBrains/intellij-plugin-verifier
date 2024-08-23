@file:Suppress("unused")

package mock.plugin.property

import org.jetbrains.annotations.PropertyKey

enum class Verbosity(@PropertyKey(resourceBundle = BUNDLE) val titleKey: String) {
  QUIET("mock.plugin.linter.quiet"),
  VERBOSE("mock.plugin.linter.verbose")
}

private const val BUNDLE = "mock.plugin.property.linter"