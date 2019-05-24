package defaults.kotlin

/**
 * https://youtrack.jetbrains.com/issue/KT-4779
 */
interface I {

  @JvmDefault
  fun withDefault(): Int = 0

  fun noDefault(): Int = 0

}