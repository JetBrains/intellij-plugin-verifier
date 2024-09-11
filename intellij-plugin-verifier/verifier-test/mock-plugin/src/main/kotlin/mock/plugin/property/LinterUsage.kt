@file:Suppress("unused")

package mock.plugin.property

class LinterUsage {
  fun useLinter(): Linter {
    return Linter.QUIET
  }
}