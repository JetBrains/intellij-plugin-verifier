package com.jetbrains.plugin.structure.classes.resolvers

import java.nio.file.Path

interface ClassFileOrigin {
  val parent: ClassFileOrigin?
}

inline fun <reified T : ClassFileOrigin> ClassFileOrigin.findOriginOfType(): T? =
    generateSequence(this) { it.parent }.filterIsInstance<T>().firstOrNull()

inline fun <reified T : ClassFileOrigin> ClassFileOrigin.isOriginOfType(): Boolean = findOriginOfType<T>() != null

data class JarClassFileOrigin(val jarName: String, override val parent: ClassFileOrigin) : ClassFileOrigin

data class JdkClassFileOrigin(val jdkPath: Path) : ClassFileOrigin {
  override val parent: ClassFileOrigin? = null
}