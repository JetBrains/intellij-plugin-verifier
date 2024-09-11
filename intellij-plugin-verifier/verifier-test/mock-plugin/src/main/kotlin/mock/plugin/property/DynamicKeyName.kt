@file:Suppress("unused")

package mock.plugin.property

import org.jetbrains.annotations.PropertyKey

enum class DynamicKeyName(@PropertyKey(resourceBundle = "mock.plugin.property.linter") val titleKey: String) {
  QUIET(prefix() + "." + quiet()),
  VERBOSE(prefix() + "." + verbose())
}

private fun prefix() = "mock.plugin.linter"
private fun quiet() = "quiet"
private fun verbose() = "verbose"