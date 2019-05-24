package com.jetbrains.pluginverifier.verifiers.resolution

fun isClassAccessibleToOtherClass(me: ClassFile, other: ClassFile): Boolean =
    me.isPublic
        || me.isPrivate && me.name == other.name
        || me.javaPackageName == other.javaPackageName
        || isKotlinDefaultConstructorMarker(me)

/**
 * In Kotlin classes the default constructor has a special parameter of type `DefaultConstructorMarker`.
 * This class is package-private but is never instantiated because `null` is always passed as its value.
 * We should not report "illegal access" for this class.
 */
private fun isKotlinDefaultConstructorMarker(classFile: ClassFile): Boolean =
    classFile.name == "kotlin/jvm/internal/DefaultConstructorMarker"