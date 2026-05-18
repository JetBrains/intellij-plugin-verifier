package overrideOnly

import org.jetbrains.annotations.ApiStatus

/**
 * Kotlin top-level `object`. Methods cannot be overridden by callers, so a
 * `@OverrideOnly` annotation on them is contradictory and callers must not be flagged.
 */
object OverrideOnlyKotlinObject {
  @ApiStatus.OverrideOnly
  fun overrideOnlyMethod() {
  }
}

/**
 * Kotlin class with a `companion object` carrying a `@OverrideOnly` method. The
 * companion object compiles to a singleton holder whose methods are not overridable
 * by callers, so callers must not be flagged.
 */
class OverrideOnlyKotlinCompanionHolder {
  companion object {
    @ApiStatus.OverrideOnly
    fun overrideOnlyMethod() {
    }
  }
}

/**
 * `@OverrideOnly` interface that exposes a factory method on its `companion object`.
 *
 * `isSatisfied` is a regular instance method on the interface and calling it from
 * outside the implementor remains a violation. The companion factory `create` must
 * not be flagged: its containing class is
 * `OverrideOnlyKotlinInterfaceWithCompanion$Companion`, not the interface itself,
 * so it picks up the annotation only via the enclosing-class lookup, and callers
 * cannot override a singleton's methods.
 */
@ApiStatus.OverrideOnly
interface OverrideOnlyKotlinInterfaceWithCompanion {
  fun isSatisfied(): Boolean

  companion object {
    fun create(): OverrideOnlyKotlinInterfaceWithCompanion = throw NotImplementedError()
  }
}
