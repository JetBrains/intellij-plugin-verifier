package com.jetbrains.pluginverifier.results.modifiers

import java.io.Serializable

data class Modifiers(private val flags: Int) : Serializable {

  enum class Modifier(val flag: Int) {
    PUBLIC(0x0001), // class, field, method
    PRIVATE(0x0002), // class, field, method
    PROTECTED(0x0004), // class, field, method
    STATIC(0x0008), // field, method
    FINAL(0x0010), // class, field, method, parameter
    SUPER(0x0020), // class
    SYNCHRONIZED(0x0020), // method
    VOLATILE(0x0040), // field
    BRIDGE(0x0040), // method
    VARARGS(0x0080), // method
    TRANSIENT(0x0080), // field
    NATIVE(0x0100), // method
    INTERFACE(0x0200), // class
    ABSTRACT(0x0400), // class, method
    STRICT(0x0800), // method
    SYNTHETIC(0x1000), // class, field, method, parameter
    ANNOTATION(0x2000), // class
    ENUM(0x4000), // class(?) field inner
    MANDATED(0x8000), // parameter
    DEPRECATED(0x20000) // class, field, method
  }

  fun contains(flag: Modifier): Boolean = flags.and(flag.flag) != 0

  companion object {
    fun of(vararg modifiers: Modifier) = Modifiers(modifiers.map { it.flag }.fold(0) { acc, m -> acc.or(m) })
  }

}