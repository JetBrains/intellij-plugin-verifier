package com.jetbrains.pluginverifier.verifiers

import org.objectweb.asm.tree.ClassNode

private val JDK_8_SPECIFIC_PACKAGES = listOf(
    "javax.activation",
    "javax.xml.bind",
    "javax.annotation",
    "com.sun.activation",
    "com.sun.istack",
    "com.sun.xml.bind",
    "com.sun.xml.fastinfoset",
    "com.sun.xml.txw2",
    "org.jvnet.fastinfoset",
    "org.jvnet.staxex",

    "javax.jws",
    "javax.xml.soap",
    "javax.xml.ws",
    "javax.activity",
    "javax.rmi",
    "org.omg",
    "javax.transaction"
)

fun ClassNode.isDiscouragingJdkClass(): Boolean {
  val packageName = getJavaPackage()
  return JDK_8_SPECIFIC_PACKAGES.any { packageName == it || packageName.startsWith("$it.") }
}