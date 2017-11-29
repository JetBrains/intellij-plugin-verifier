package com.jetbrains.pluginverifier.verifiers.filter

import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescr
import com.jetbrains.pluginverifier.verifiers.getInvisibleAnnotations
import org.objectweb.asm.tree.ClassNode

/**
 * Verification filter that excludes classes marked with [com.intellij.ide.plugins.DynamicallyLoaded] annotation.
 * This annotation indicates that the class is loaded by custom class loader, thus
 * it may be impossible to statically analyse its bytecode.
 */
class DynamicallyLoadedFilter : VerificationFilter {

  companion object {
    private val DYNAMICALLY_LOADED = "com/intellij/ide/plugins/DynamicallyLoaded"
  }

  override fun shouldVerify(classNode: ClassNode) =
      classNode.getInvisibleAnnotations().none { it.desc.extractClassNameFromDescr() == DYNAMICALLY_LOADED }

}