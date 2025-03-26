package com.jetbrains.plugin.structure.base.utils

import net.bytebuddy.ByteBuddy

internal fun ByteBuddy.emptyClass(fullyQualifiedName: String): ByteArray {
  return subclass(Object::class.java)
    .name(fullyQualifiedName)
    .make()
    .bytes
}