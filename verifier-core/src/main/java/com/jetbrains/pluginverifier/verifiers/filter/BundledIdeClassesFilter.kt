package com.jetbrains.pluginverifier.verifiers.filter

import com.jetbrains.plugin.structure.ide.util.KnownIdePackages
import org.objectweb.asm.tree.ClassNode

/**
 * Verification filter that excludes mistakenly bundled IDE classes.
 */
object BundledIdeClassesFilter : ClassFilter {

  override fun shouldVerify(classNode: ClassNode): Boolean {
    val packageName = classNode.name.substringBeforeLast('/', "").replace('/', '.')
    return !KnownIdePackages.isKnownPackage(packageName)
  }

}