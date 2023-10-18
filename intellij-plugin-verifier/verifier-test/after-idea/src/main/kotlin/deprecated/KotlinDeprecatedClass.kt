package deprecated

/**
 * A class annotated with `kotlin.Deprecated` annotation instead of JVM `java.lang.Deprecated`.
 *
 * The annotation is compiled into class as per
 * [Java Virtual Machine Specification](https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.15).
 */
@Deprecated(message = "No longer available in the after-idea")
class KotlinDeprecatedClass() {
  @Deprecated(message = "Use the default constructor", level = DeprecationLevel.HIDDEN)
  @Suppress("unused")
  constructor(x: Int) : this() {
    // no-op
  }
}