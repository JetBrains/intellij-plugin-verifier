package mock.plugin.property

import org.jetbrains.annotations.PropertyKey

/*expected(PROBLEM)
Reference to a missing property NONEXISTENT of resource bundle mock.plugin.property.linter

Method mock.plugin.property.NonExistentPropertyKeyHost.<clinit>() : void references property NONEXISTENT that is not found in resource bundle mock.plugin.property.linter. This can lead to **MissingResourceException** exception at runtime.
*/
enum class NonExistentPropertyKeyHost(@PropertyKey(resourceBundle = "mock.plugin.property.linter") val titleKey: String) {
  NONEXISTENT("NONEXISTENT"),
}