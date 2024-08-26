@file:Suppress("unused")

package mock.plugin.property

import org.jetbrains.annotations.PropertyKey

enum class Linter(@PropertyKey(resourceBundle = "mock.plugin.property.linter") val titleKey: String) {
  QUIET("mock.plugin.linter.quiet"),
  VERBOSE("mock.plugin.linter.verbose")
}