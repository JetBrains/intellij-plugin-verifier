package defaults.kotlin

/**
 * https://youtrack.jetbrains.com/issue/KT-4779
 */
interface I {

  @Suppress("DEPRECATION")
  @JvmDefault
  fun withDefault(): Int = 0

  fun noDefault(): Int = 0

}